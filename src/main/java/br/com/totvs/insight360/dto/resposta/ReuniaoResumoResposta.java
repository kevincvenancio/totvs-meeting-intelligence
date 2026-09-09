package br.com.totvs.insight360.dto.resposta;

import br.com.totvs.insight360.model.RiscoChurn;
import br.com.totvs.insight360.model.Sentimento;
import br.com.totvs.insight360.model.StatusCompletude;

import java.time.LocalDate;

/**
 * Reunião em formato de listagem — campos suficientes para a grade do Front-End.
 */
public record ReuniaoResumoResposta(
        Long id,
        String idExterno,
        LocalDate data,
        String cliente,
        String vendedor,
        String categoriaPrincipal,
        StatusCompletude statusCompletude,
        Sentimento sentimento,
        String sentimentoFormatado,
        RiscoChurn riscoChurn,
        String riscoChurnLabel,
        String prioridade,
        Integer scoreComercial,
        Integer scoreQualidade,
        String duracao,
        boolean analisada,
        boolean duplicada,
        String codigoLote,
        ClienteResumoResposta clienteVinculado
) {
}
