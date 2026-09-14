package br.com.totvs.hermes.dto.resposta;

import java.util.List;

/**
 * Resumo devolvido ao Front-End após o upload de um CSV de transcrições.
 */
public record ResultadoImportacaoResposta(
        ImportacaoLoteResposta lote,
        int totalEncontradas,
        int processadas,
        int incompletas,
        int duplicadas,
        int erros,
        List<String> errosDetalhe
) {
}
