package br.com.totvs.hermes.exception;

/**
 * Lançada quando os dados são sintaticamente válidos, mas ferem uma regra de
 * negócio (transição de status inválida, prazo no passado, etc.).
 * Traduzida para HTTP 422 (Unprocessable Entity).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
