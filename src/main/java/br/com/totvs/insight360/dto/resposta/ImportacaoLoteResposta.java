package br.com.totvs.insight360.dto.resposta;

import java.time.LocalDateTime;

/**
 * Lote de importação de CSV.
 */
public record ImportacaoLoteResposta(
        Long id,
        String codigoLote,
        String nomeArquivoOriginal,
        LocalDateTime dataHoraImportacao,
        int totalRegistrosBrutos,
        int totalReunioesValidas,
        int totalReunioesIncompletas,
        int totalReunioesDuplicadas,
        int totalReunioesComErro,
        String statusProcessamento,
        String observacoes
) {
}
