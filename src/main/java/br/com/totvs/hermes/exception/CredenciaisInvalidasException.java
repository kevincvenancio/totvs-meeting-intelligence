package br.com.totvs.hermes.exception;

/**
 * Lançada quando o e-mail não existe, a senha não confere ou o usuário está
 * inativo. Traduzida para HTTP 401 (Unauthorized).
 * <p>
 * A mensagem é sempre genérica, para não revelar quais e-mails estão cadastrados.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
