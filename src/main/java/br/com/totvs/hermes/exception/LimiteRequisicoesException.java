package br.com.totvs.hermes.exception;

/**
 * O mesmo IP passou do limite de perguntas por minuto ao chat.
 * Convertida em HTTP 429 pelo {@link ManipuladorGlobalExcecoes}.
 */
public class LimiteRequisicoesException extends RuntimeException {

    public LimiteRequisicoesException() {
        super("Muitas perguntas seguidas. Aguarde um minuto e tente novamente.");
    }
}
