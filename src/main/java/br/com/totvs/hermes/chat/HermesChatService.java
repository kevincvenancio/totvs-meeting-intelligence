package br.com.totvs.hermes.chat;

import br.com.totvs.hermes.chat.llm.LlmClient;
import br.com.totvs.hermes.chat.llm.Mensagem;
import br.com.totvs.hermes.dto.requisicao.ChatRequisicao;
import br.com.totvs.hermes.dto.resposta.ChatResposta;
import br.com.totvs.hermes.exception.HermesIndisponivelException;
import br.com.totvs.hermes.exception.LimiteRequisicoesException;
import br.com.totvs.hermes.exception.PerguntaInvalidaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestra uma pergunta ao Hermes:
 * <ol>
 *   <li>valida entrada e rate limit;</li>
 *   <li>{@link EscopoGuard#avaliar pré-filtro} — barra off-topic/injeção sem gastar cota;</li>
 *   <li>{@link ContextoHermesService} — dados reais do banco;</li>
 *   <li>{@link LlmClient} — OpenRouter → fallback Groq/Cerebras/Ollama;</li>
 *   <li>{@link EscopoGuard#filtrarResposta pós-filtro}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HermesChatService {

    private static final Set<String> PAPEIS_ACEITOS = Set.of("user", "assistant");
    private static final int MAX_CARACTERES_TURNO_HISTORICO = 1_000;
    private static final Pattern CITACAO_REUNIAO = Pattern.compile("(?iu)reuni[aã]o\\s*#\\s*(\\d{1,9})");
    private static final Pattern CITACAO_PLANO = Pattern.compile("(?iu)plano\\s*#\\s*(\\d{1,9})");

    private final HermesChatPropriedades propriedades;
    private final EscopoGuard escopoGuard;
    private final ContextoHermesService contextoService;
    private final LlmClient llm;
    private final LimiteRequisicoes limiteRequisicoes;

    public ChatResposta responder(ChatRequisicao requisicao, String ip) {
        if (!propriedades.habilitado()) throw new HermesIndisponivelException();
        long inicio = System.currentTimeMillis();
        boolean suporte = requisicao.suporte();
        String agente = suporte ? "SUPORTE" : "REUNIOES";

        String pergunta = requisicao.mensagem() == null ? "" : requisicao.mensagem().trim();
        if (pergunta.isEmpty() || pergunta.length() > propriedades.maxCaracteresPergunta()) {
            throw new PerguntaInvalidaException("A pergunta deve ter entre 1 e "
                    + propriedades.maxCaracteresPergunta() + " caracteres.");
        }
        if (!limiteRequisicoes.permitir(ip)) throw new LimiteRequisicoesException();

        List<Mensagem> historico = historicoSeguro(requisicao.historico());
        boolean continuacao = historico.stream()
                .anyMatch(m -> "user".equals(m.papel()) && escopoGuard.temDominio(m.conteudo(), suporte));

        // ── 1. Pré-filtro: nada de LLM para o que já sabemos recusar ──
        EscopoGuard.Avaliacao avaliacao = escopoGuard.avaliar(pergunta, continuacao, suporte);
        if (avaliacao.bloqueada()) {
            log.info("Hermes Chat: pergunta barrada no pré-filtro ({})", avaliacao.veredito());
            return ChatResposta.fixa(avaliacao.respostaFixa(), avaliacao.foraDoEscopo(), agente, decorrido(inicio));
        }

        // ── 2. Contexto: dados reais (Reuniões) ou só o guia das telas (Suporte) ──
        ContextoHermesService.Contexto contexto = suporte
                ? contextoService.montarSuporte()
                : contextoService.montar(pergunta, requisicao.reuniaoId(), requisicao.clienteId());

        // ── 3. LLM ──
        String sistema = suporte
                ? PromptHermes.suporte(requisicao.tela(), contexto.texto())
                : PromptHermes.comContexto(contexto.texto());
        List<Mensagem> mensagens = new ArrayList<>();
        mensagens.add(Mensagem.sistema(sistema));
        mensagens.addAll(historico);
        mensagens.add(Mensagem.usuario(pergunta));
        String bruta = llm.completar(mensagens);

        // ── 4. Pós-filtro ──
        EscopoGuard.RespostaFiltrada filtrada =
                escopoGuard.filtrarResposta(bruta, contexto.idsReunioes(), contexto.idsPlanos());
        if (filtrada.substituida()) {
            log.warn("Hermes Chat: resposta do modelo substituída no pós-filtro");
            return ChatResposta.fixa(filtrada.texto(), true, agente, decorrido(inicio));
        }
        boolean recusou = filtrada.texto().startsWith(PromptHermes.RECUSA.substring(0, 40));
        ChatResposta.Meta meta = new ChatResposta.Meta(agente,
                contexto.idsReunioes().size(), contexto.idsPlanos().size(),
                contexto.texto().length() / 4, decorrido(inicio), true);
        return new ChatResposta(filtrada.texto(), fontes(filtrada.texto(), contexto, requisicao), recusou, meta);
    }

    private static long decorrido(long inicio) {
        return System.currentTimeMillis() - inicio;
    }

    /**
     * Só {@code user}/{@code assistant}, nunca {@code system} vindo do cliente;
     * últimos N turnos; cada texto cortado.
     */
    List<Mensagem> historicoSeguro(List<ChatRequisicao.MensagemHistorico> historico) {
        if (historico == null || historico.isEmpty()) return List.of();
        List<Mensagem> validas = historico.stream()
                .filter(h -> h != null && h.papel() != null && h.texto() != null && !h.texto().isBlank())
                .filter(h -> PAPEIS_ACEITOS.contains(h.papel().toLowerCase()))
                .map(h -> new Mensagem(h.papel().toLowerCase(), cortar(h.texto().trim())))
                .toList();
        int inicio = Math.max(0, validas.size() - propriedades.maxTurnosHistorico());
        return validas.subList(inicio, validas.size());
    }

    /** Registros que a resposta de fato citou (ou o que estava aberto na tela). */
    private List<ChatResposta.Fonte> fontes(String resposta, ContextoHermesService.Contexto contexto,
                                            ChatRequisicao requisicao) {
        Set<ChatResposta.Fonte> fontes = new LinkedHashSet<>();

        Matcher m = CITACAO_REUNIAO.matcher(resposta);
        while (m.find()) {
            Long id = Long.valueOf(m.group(1));
            if (contexto.idsReunioes().contains(id)) fontes.add(new ChatResposta.Fonte("REUNIAO", id));
        }
        m = CITACAO_PLANO.matcher(resposta);
        while (m.find()) {
            Long id = Long.valueOf(m.group(1));
            if (contexto.idsPlanos().contains(id)) fontes.add(new ChatResposta.Fonte("PLANO_ACAO", id));
        }
        contexto.titulosPlanos().forEach((titulo, id) -> {
            if (titulo.length() >= 6 && resposta.contains(titulo)) fontes.add(new ChatResposta.Fonte("PLANO_ACAO", id));
        });

        if (requisicao.reuniaoId() != null && contexto.idsReunioes().contains(requisicao.reuniaoId())) {
            fontes.add(new ChatResposta.Fonte("REUNIAO", requisicao.reuniaoId()));
        }
        contexto.idsClientes().forEach(id -> fontes.add(new ChatResposta.Fonte("CLIENTE", id)));
        return List.copyOf(fontes);
    }

    private static String cortar(String texto) {
        return texto.length() <= MAX_CARACTERES_TURNO_HISTORICO ? texto
                : texto.substring(0, MAX_CARACTERES_TURNO_HISTORICO);
    }
}
