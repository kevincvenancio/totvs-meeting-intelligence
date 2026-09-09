package br.com.totvs.insight360.exception;

/**
 * Lançada quando o arquivo enviado para importação é inválido
 * (vazio, sem extensão .csv ou ilegível). Traduzida para HTTP 400 (Bad Request).
 */
public class ArquivoInvalidoException extends RuntimeException {

    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }

    public ArquivoInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
