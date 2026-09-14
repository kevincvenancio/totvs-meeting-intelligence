package br.com.totvs.hermes.dto.resposta;

/**
 * Insight extraído automaticamente da transcrição de uma reunião.
 */
public record InsightResposta(
        Long id,
        Long reuniaoId,
        String tipo,
        String descricao,
        String prioridade,
        String trechoOrigem,
        Integer confianca
) {
}
