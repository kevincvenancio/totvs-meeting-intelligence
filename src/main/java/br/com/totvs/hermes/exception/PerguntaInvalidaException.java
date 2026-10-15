package br.com.totvs.hermes.exception;

/**
 * Pergunta ao chat vazia ou maior que o limite configurado. Convertida em HTTP 400.
 */
public class PerguntaInvalidaException extends RuntimeException {

    public PerguntaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
