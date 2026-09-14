package br.com.totvs.hermes.exception;

/**
 * Lançada quando a operação viola uma restrição de unicidade do domínio
 * (e-mail de usuário ou CNPJ de cliente já cadastrado, por exemplo).
 * Traduzida para HTTP 409 (Conflict).
 */
public class ConflitoDeDadosException extends RuntimeException {

    public ConflitoDeDadosException(String mensagem) {
        super(mensagem);
    }
}
