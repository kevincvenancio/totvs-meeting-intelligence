package br.com.totvs.insight360.dto.resposta;

import java.util.List;
import java.util.Map;

/**
 * Painel executivo consolidado — reúne os indicadores da análise de reuniões com
 * os indicadores de acompanhamento comercial (planos de ação e carteira).
 *
 * @param totalReunioes          reuniões analisadas
 * @param totalClientesCitados   clientes distintos citados nas reuniões
 * @param totalChurnAlto         reuniões com risco de churn alto
 * @param totalChurnMedio        reuniões com risco de churn médio
 * @param totalOportunidades     reuniões com oportunidade identificada
 * @param scoreQualidadeMedia    média do score de qualidade
 * @param scoreComercialMedia    média do score comercial
 * @param sentimentos            distribuição de sentimentos
 * @param sentimentoPredominante sentimento mais frequente
 * @param topProdutos            produtos mais citados
 * @param topConcorrentes        concorrentes mais citados
 * @param topCategorias          categorias de reunião mais frequentes
 * @param topOportunidades       reuniões com maior score comercial
 * @param topRiscoChurn          reuniões com maior risco de churn
 * @param planosAcao             indicadores dos planos de ação
 * @param clientes               indicadores da carteira de clientes
 */
public record DashboardResposta(
        long totalReunioes,
        long totalClientesCitados,
        long totalChurnAlto,
        long totalChurnMedio,
        long totalOportunidades,
        String scoreQualidadeMedia,
        String scoreComercialMedia,
        Map<String, Long> sentimentos,
        String sentimentoPredominante,
        Map<String, Long> topProdutos,
        Map<String, Long> topConcorrentes,
        Map<String, Long> topCategorias,
        List<ReuniaoResumoResposta> topOportunidades,
        List<ReuniaoResumoResposta> topRiscoChurn,
        IndicadoresPlanoAcaoResposta planosAcao,
        IndicadoresClienteResposta clientes
) {
}
