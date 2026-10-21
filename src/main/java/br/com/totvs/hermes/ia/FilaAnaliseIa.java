package br.com.totvs.hermes.ia;

import br.com.totvs.hermes.model.Insight;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.repository.InsightRepository;
import br.com.totvs.hermes.repository.ReuniaoRepository;
import br.com.totvs.hermes.service.FeedbackReuniaoService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fila de análise por IA em segundo plano.
 * <p>
 * A importação de um CSV grava as reuniões com a análise das regras na hora e coloca os
 * ids aqui; uma thread única manda as transcrições ao serviço de IA em lotes (o modelo
 * processa os trechos de várias reuniões de uma vez, o que rende mais por reunião) e
 * grava o resultado. Assim a importação termina em segundos e a base inteira passa pelo
 * modelo enquanto o sistema continua em uso.
 * <p>
 * Se a IA cair, o lote volta para a fila e a thread tenta de novo depois de
 * {@link #ESPERA_IA_FORA_MS} — nenhuma reunião é perdida.
 */
@Service
@Slf4j
public class FilaAnaliseIa {

    static final long ESPERA_IA_FORA_MS = 30_000;
    /** Espera entre tentativas quando a IA está fora (ajustável nos testes). */
    long esperaIaForaMs = ESPERA_IA_FORA_MS;

    public enum Estado { DESLIGADA, PARADA, PROCESSANDO, AGUARDANDO_IA }

    public record StatusFila(boolean habilitada, Estado estado, int pendentes, long processadas, long falhas,
                             Double segundosPorReuniao, Long minutosRestantes,
                             LocalDateTime ultimaAtualizacao, String ultimoErro) {
    }

    private final HermesIaClient ia;
    private final ReuniaoRepository reuniaoRepository;
    private final InsightRepository insightRepository;
    private final FeedbackReuniaoService feedbackService;
    private final TransactionTemplate transacao;
    private final int tamanhoLote;

    /** Ids aguardando, sem repetição e na ordem de chegada. Protegido por {@code this}. */
    private final LinkedHashSet<Long> pendentes = new LinkedHashSet<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(tarefa -> {
        Thread t = new Thread(tarefa, "hermes-ia-fila");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean rodando = new AtomicBoolean(false);
    private final AtomicLong processadas = new AtomicLong();
    private final AtomicLong falhas = new AtomicLong();
    private volatile Estado estado = Estado.PARADA;
    private volatile Double segundosPorReuniao;
    private volatile LocalDateTime ultimaAtualizacao;
    private volatile String ultimoErro;

    public FilaAnaliseIa(HermesIaClient ia, ReuniaoRepository reuniaoRepository, InsightRepository insightRepository,
                         FeedbackReuniaoService feedbackService, PlatformTransactionManager gerenciadorTransacao,
                         HermesIaPropriedades propriedades) {
        this.ia = ia;
        this.reuniaoRepository = reuniaoRepository;
        this.insightRepository = insightRepository;
        this.feedbackService = feedbackService;
        this.transacao = new TransactionTemplate(gerenciadorTransacao);
        this.tamanhoLote = propriedades.lote();
    }

    // ── Entrada ───────────────────────────────────────────────────────

    /**
     * Coloca reuniões na fila. Chamado dentro da transação da importação, os ids só
     * entram depois do commit — antes disso a thread da fila não enxergaria as linhas.
     */
    public void enfileirar(Collection<Long> ids) {
        if (!ia.habilitado() || ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> copia = List.copyOf(ids);
        Runnable acao = () -> {
            synchronized (this) {
                pendentes.addAll(copia);
            }
            disparar();
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    acao.run();
                }
            });
        } else {
            acao.run();
        }
    }

    public void enfileirar(Long id) {
        enfileirar(List.of(id));
    }

    /**
     * Coloca a base na fila.
     *
     * @param todas {@code false} = só as reuniões que ainda não passaram pela IA;
     *              {@code true} = reprocessa todas (ex.: depois de trocar de modelo)
     * @return quantas reuniões entraram na fila
     */
    public int enfileirarBase(boolean todas) {
        if (!ia.habilitado()) {
            throw new IaIndisponivelException(
                    "A IA não está configurada. Suba o serviço Python e defina HERMES_IA_URL.");
        }
        List<Long> ids = reuniaoRepository.findIdsParaAnaliseIa(todas);
        enfileirar(ids);
        log.info("Fila de IA: {} reunião(ões) enfileirada(s) ({})", ids.size(), todas ? "todas" : "só as sem IA");
        return ids.size();
    }

    public StatusFila status() {
        int qtd;
        synchronized (this) {
            qtd = pendentes.size();
        }
        Double ritmo = segundosPorReuniao;
        Long restante = ritmo == null || qtd == 0 ? null : Math.max(1, Math.round(qtd * ritmo / 60.0));
        Estado atual = !ia.habilitado() ? Estado.DESLIGADA : estado;
        return new StatusFila(ia.habilitado(), atual, qtd, processadas.get(), falhas.get(),
                ritmo == null ? null : Math.round(ritmo * 100) / 100.0, restante, ultimaAtualizacao, ultimoErro);
    }

    // ── Processamento ─────────────────────────────────────────────────

    private void disparar() {
        if (rodando.compareAndSet(false, true)) {
            estado = Estado.PROCESSANDO;
            executor.submit(this::processar);
        }
    }

    private void processar() {
        try {
            List<Long> lote;
            while (!(lote = proximoLote()).isEmpty()) {
                estado = Estado.PROCESSANDO;
                try {
                    processarLote(lote);
                    ultimoErro = null;
                } catch (IaIndisponivelException e) {
                    devolver(lote);
                    estado = Estado.AGUARDANDO_IA;
                    ultimoErro = e.getMessage();
                    log.warn("Fila de IA: {} — nova tentativa em {} s", e.getMessage(), esperaIaForaMs / 1000);
                    Thread.sleep(esperaIaForaMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            ultimoErro = e.getMessage();
            log.error("Fila de IA parou por erro inesperado", e);
        } finally {
            estado = Estado.PARADA;
            rodando.set(false);
            boolean sobrou;
            synchronized (this) {
                sobrou = !pendentes.isEmpty();
            }
            if (sobrou && !Thread.currentThread().isInterrupted()) {
                disparar();
            }
        }
    }

    private void processarLote(List<Long> ids) {
        long inicio = System.nanoTime();

        // 1. Lê as transcrições (transação curta, só leitura)
        Map<Long, String> textos = new LinkedHashMap<>();
        transacao.executeWithoutResult(s -> {
            for (Reuniao r : reuniaoRepository.findAllById(ids)) {
                String texto = r.getTranscricaoTratada();
                if (texto == null || texto.isBlank()) {
                    texto = r.getTranscricaoOriginal();
                }
                if (texto != null && !texto.isBlank()) {
                    textos.put(r.getId(), texto);
                }
            }
        });
        if (textos.isEmpty()) {
            return;
        }

        // 2. Modelo — fora de transação, para não segurar conexão do pool durante a inferência
        List<JsonNode> resultados = ia.analisarLote(new ArrayList<>(textos.values()));

        // 3. Grava cada reunião na própria transação (uma falha não derruba o lote)
        Iterator<JsonNode> it = resultados.iterator();
        for (Long id : textos.keySet()) {
            JsonNode resultado = it.next();
            try {
                transacao.executeWithoutResult(s -> gravar(id, resultado));
                processadas.incrementAndGet();
            } catch (RuntimeException e) {
                falhas.incrementAndGet();
                ultimoErro = "Reunião " + id + ": " + e.getMessage();
                log.warn("Fila de IA: falha ao gravar a reunião {}: {}", id, e.getMessage());
            }
        }

        double segundos = (System.nanoTime() - inicio) / 1e9 / textos.size();
        segundosPorReuniao = segundosPorReuniao == null ? segundos : 0.8 * segundosPorReuniao + 0.2 * segundos;
        ultimaAtualizacao = LocalDateTime.now();
    }

    private void gravar(Long id, JsonNode resultado) {
        Reuniao reuniao = reuniaoRepository.findById(id).orElse(null);
        if (reuniao == null) {
            return; // removida enquanto esperava na fila
        }
        insightRepository.deleteSubstituidosPelaIa(id, "%(IA)", "Risco de churn —%");
        List<Insight> novos = AplicadorAnaliseIa.aplicar(reuniao, resultado);
        insightRepository.saveAll(novos);
        reuniaoRepository.save(reuniao);
        // O feedback educativo usa churn e sentimento: regenera com o resultado da IA
        feedbackService.gerarFeedback(reuniao);
    }

    private synchronized List<Long> proximoLote() {
        List<Long> lote = new ArrayList<>(tamanhoLote);
        Iterator<Long> it = pendentes.iterator();
        while (it.hasNext() && lote.size() < tamanhoLote) {
            lote.add(it.next());
            it.remove();
        }
        return lote;
    }

    /** Devolve o lote para o começo da fila. */
    private synchronized void devolver(List<Long> lote) {
        LinkedHashSet<Long> nova = new LinkedHashSet<>(lote);
        nova.addAll(pendentes);
        pendentes.clear();
        pendentes.addAll(nova);
    }

    @PreDestroy
    void encerrar() {
        executor.shutdownNow();
    }
}
