package br.com.totvs.insight360.dto.mapper;

import br.com.totvs.insight360.dto.resposta.ImportacaoLoteResposta;
import br.com.totvs.insight360.dto.resposta.ResultadoImportacaoResposta;
import br.com.totvs.insight360.model.ImportacaoLote;
import br.com.totvs.insight360.service.ResultadoImportacao;

import java.util.List;

/**
 * Conversões dos lotes de importação para os DTOs da API.
 */
public final class ImportacaoMapper {

    private ImportacaoMapper() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    public static ImportacaoLoteResposta paraResposta(ImportacaoLote lote) {
        if (lote == null) {
            return null;
        }
        return new ImportacaoLoteResposta(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getNomeArquivoOriginal(),
                lote.getDataHoraImportacao(),
                lote.getTotalRegistrosBrutos(),
                lote.getTotalReunioesValidas(),
                lote.getTotalReunioesIncompletas(),
                lote.getTotalReunioesDuplicadas(),
                lote.getTotalReunioesComErro(),
                lote.getStatusProcessamento(),
                lote.getObservacoes());
    }

    public static List<ImportacaoLoteResposta> paraLista(List<ImportacaoLote> lotes) {
        return lotes.stream().map(ImportacaoMapper::paraResposta).toList();
    }

    public static ResultadoImportacaoResposta paraResposta(ResultadoImportacao resultado) {
        return new ResultadoImportacaoResposta(
                paraResposta(resultado.lote()),
                resultado.totalEncontradas(),
                resultado.processadas(),
                resultado.incompletas(),
                resultado.duplicadas(),
                resultado.erros(),
                resultado.errosDetalhe());
    }
}
