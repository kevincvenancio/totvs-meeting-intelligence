package br.com.totvs.hermes.service;

/**
 * Relatório em PDF já materializado em memória, pronto para ser devolvido pela API.
 *
 * @param nomeArquivo nome sugerido para download
 * @param conteudo    bytes do PDF
 */
public record ArquivoRelatorio(String nomeArquivo, byte[] conteudo) {

    /** Tamanho do arquivo em bytes. */
    public int tamanho() {
        return conteudo != null ? conteudo.length : 0;
    }
}
