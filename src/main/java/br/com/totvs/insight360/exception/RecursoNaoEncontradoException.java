package br.com.totvs.insight360.exception;

/**
 * Lançada quando um recurso solicitado não existe na base.
 * Traduzida pelo {@link ManipuladorGlobalExcecoes} para HTTP 404 (Not Found).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    /**
     * Fábrica padronizada para a mensagem "Recurso não encontrado: id".
     *
     * @param recurso nome do recurso (ex.: "Cliente")
     * @param id      identificador procurado
     */
    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(recurso + " não encontrado(a) para o id " + id + ".");
    }
}
