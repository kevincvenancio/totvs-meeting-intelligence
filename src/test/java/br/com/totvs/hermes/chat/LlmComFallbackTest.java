package br.com.totvs.hermes.chat;

import br.com.totvs.hermes.chat.llm.LlmComFallback;
import br.com.totvs.hermes.chat.llm.Mensagem;
import br.com.totvs.hermes.chat.llm.ProvedorLlm;
import br.com.totvs.hermes.chat.llm.ProvedorLlmException;
import br.com.totvs.hermes.exception.HermesIndisponivelException;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * Testes manuais do fallback entre provedores e do rate limit.
 * Padrão do projeto: apenas try-catch, sem JUnit/AssertJ/Mockito; provedores são stubs.
 */
public class LlmComFallbackTest {

    private static int passou = 0, falhou = 0;
    private static final List<Mensagem> CONVERSA = List.of(Mensagem.usuario("Quais planos estão atrasados?"));

    /** Stub: responde um texto fixo ou falha com o status HTTP informado. */
    static class ProvedorStub implements ProvedorLlm {
        final String nome;
        final String chave;
        final Integer falhaComStatus;
        int chamadas = 0;

        ProvedorStub(String nome, String chave, Integer falhaComStatus) {
            this.nome = nome;
            this.chave = chave;
            this.falhaComStatus = falhaComStatus;
        }

        @Override public String nome() { return nome; }
        @Override public boolean configurado() { return chave != null && !chave.isBlank(); }

        @Override
        public String completar(List<Mensagem> mensagens) {
            chamadas++;
            if (falhaComStatus != null) throw new ProvedorLlmException(nome + " HTTP " + falhaComStatus, falhaComStatus);
            return "resposta do " + nome;
        }
    }

    public static void main(String[] args) {
        verificar("Provedor 1 com 429 → resposta vem do provedor 2", () -> {
            ProvedorStub p1 = new ProvedorStub("openrouter", "k1", 429);
            ProvedorStub p2 = new ProvedorStub("groq", "k2", null);
            String r = new LlmComFallback(List.of(p1, p2)).completar(CONVERSA);
            return "resposta do groq".equals(r) && p1.chamadas == 1 && p2.chamadas == 1;
        });

        verificar("Provedor 1 com 402 e 2 com 503 → provedor 3 responde", () -> {
            ProvedorStub p3 = new ProvedorStub("ollama", "ollama", null);
            String r = new LlmComFallback(List.of(new ProvedorStub("a", "k", 402),
                    new ProvedorStub("b", "k", 503), p3)).completar(CONVERSA);
            return "resposta do ollama".equals(r);
        });

        verificar("Provedor sem chave é pulado (nem é chamado)", () -> {
            ProvedorStub semChave = new ProvedorStub("openrouter", "", null);
            ProvedorStub groq = new ProvedorStub("groq", "k", null);
            LlmComFallback llm = new LlmComFallback(List.of(semChave, groq));
            return "resposta do groq".equals(llm.completar(CONVERSA))
                    && semChave.chamadas == 0 && llm.quantidadeAtivos() == 1;
        });

        verificar("Nenhuma chave configurada → HermesIndisponivelException (503)", () -> {
            try {
                new LlmComFallback(List.of(new ProvedorStub("a", null, null), new ProvedorStub("b", " ", null)))
                        .completar(CONVERSA);
                return false;
            } catch (HermesIndisponivelException e) {
                return HermesIndisponivelException.MENSAGEM.equals(e.getMessage());
            }
        });

        verificar("Todos falham → HermesIndisponivelException", () -> {
            try {
                new LlmComFallback(List.of(new ProvedorStub("a", "k", 500), new ProvedorStub("b", "k", 0)))
                        .completar(CONVERSA);
                return false;
            } catch (HermesIndisponivelException e) {
                return true;
            }
        });

        // ── Rate limit ──
        verificar("Rate limit: 8 passam, a 9ª no mesmo minuto é barrada", () -> {
            AtomicLong agora = new AtomicLong(1_000_000);
            LimiteRequisicoes limite = new LimiteRequisicoes(8, agora::get);
            for (int i = 0; i < 8; i++) if (!limite.permitir("1.2.3.4")) return false;
            return !limite.permitir("1.2.3.4") && limite.permitir("5.6.7.8");
        });

        verificar("Rate limit: libera depois de 1 minuto", () -> {
            AtomicLong agora = new AtomicLong(1_000_000);
            LimiteRequisicoes limite = new LimiteRequisicoes(2, agora::get);
            limite.permitir("ip");
            limite.permitir("ip");
            boolean barrou = !limite.permitir("ip");
            agora.addAndGet(60_001);
            return barrou && limite.permitir("ip");
        });

        System.out.println("\nResultado: " + passou + " passaram, " + falhou + " falharam.");
        if (falhou > 0) System.exit(1);
    }

    private static void verificar(String nome, BooleanSupplier teste) {
        try {
            if (!teste.getAsBoolean()) throw new AssertionError("condição falsa");
            passou++;
            System.out.println("[OK] " + nome);
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] " + nome + " — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] " + nome + " — exceção inesperada: " + e);
        }
    }
}
