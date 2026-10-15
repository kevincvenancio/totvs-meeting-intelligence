package br.com.totvs.hermes.exception;

/**
 * Nenhum provedor de LLM respondeu (ou nenhum tem chave configurada).
 * Convertida em HTTP 503 pelo {@link ManipuladorGlobalExcecoes}.
 */
public class HermesIndisponivelException extends RuntimeException {

    public static final String MENSAGEM = "O Hermes está indisponível no momento. Tente novamente em instantes.";

    public HermesIndisponivelException() {
        super(MENSAGEM);
    }
}
