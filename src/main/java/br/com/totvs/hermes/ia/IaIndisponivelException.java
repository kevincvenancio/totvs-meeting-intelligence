package br.com.totvs.hermes.ia;

/**
 * O serviço de IA não está configurado ou não respondeu.
 * Convertida em HTTP 503 pelo {@link br.com.totvs.hermes.exception.ManipuladorGlobalExcecoes}.
 */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
