package br.com.totvs.hermes.exception;

/**
 * Lançada quando o usuário informado não tem permissão para a operação
 * (por exemplo, editar um comentário de outro autor sem ser gestor/administrador).
 * Traduzida para HTTP 403 (Forbidden).
 */
public class OperacaoNaoPermitidaException extends RuntimeException {

    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
