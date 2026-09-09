package br.com.totvs.insight360.exception;

/**
 * Lançada quando ocorre falha na geração de um relatório em PDF.
 * Traduzida para HTTP 500 (Internal Server Error), preservando a causa original
 * para o log da aplicação.
 */
public class RelatorioException extends RuntimeException {

    public RelatorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
