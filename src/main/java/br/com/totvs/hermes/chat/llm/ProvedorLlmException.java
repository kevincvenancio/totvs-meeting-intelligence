package br.com.totvs.hermes.chat.llm;

/**
 * Falha de um provedor de LLM (cota, 5xx, timeout, resposta vazia...).
 * O {@link LlmComFallback} captura e tenta o próximo provedor.
 */
public class ProvedorLlmException extends RuntimeException {

    private final int status;

    public ProvedorLlmException(String mensagem, int status) {
        super(mensagem);
        this.status = status;
    }

    public ProvedorLlmException(String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.status = 0;
    }

    /** Código HTTP devolvido pelo provedor; 0 quando não houve resposta (timeout, rede). */
    public int getStatus() {
        return status;
    }
}
