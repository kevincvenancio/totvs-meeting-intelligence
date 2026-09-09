package br.com.totvs.insight360.dto.resposta;

import java.util.List;

/**
 * Envelope de paginação devolvido pelas listagens da API.
 *
 * @param conteudo       registros da página atual
 * @param pagina         número da página (base zero)
 * @param tamanho        quantidade de registros por página
 * @param totalElementos total de registros que atendem ao filtro
 * @param totalPaginas   total de páginas disponíveis
 * @param primeira       indica se é a primeira página
 * @param ultima         indica se é a última página
 * @param <T>            tipo do conteúdo
 */
public record PaginaResposta<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {

    /**
     * Monta o envelope calculando o total de páginas.
     *
     * @param conteudo       registros já mapeados para DTO
     * @param pagina         página solicitada (base zero)
     * @param tamanho        tamanho da página
     * @param totalElementos total de registros do filtro
     */
    public static <T> PaginaResposta<T> de(List<T> conteudo, int pagina, int tamanho, long totalElementos) {
        int totalPaginas = tamanho > 0 ? (int) Math.ceil((double) totalElementos / tamanho) : 0;
        boolean ultima = totalPaginas == 0 || pagina >= totalPaginas - 1;
        return new PaginaResposta<>(conteudo, pagina, tamanho, totalElementos, totalPaginas,
                pagina == 0, ultima);
    }
}
