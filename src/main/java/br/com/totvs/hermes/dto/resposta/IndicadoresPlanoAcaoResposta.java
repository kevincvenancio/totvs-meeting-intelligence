package br.com.totvs.hermes.dto.resposta;

import java.util.Map;

/**
 * Indicadores de execução dos planos de ação exibidos no dashboard.
 *
 * @param totalPorStatus quantidade de planos em cada status
 * @param total          total de planos cadastrados
 * @param emAberto       planos ainda não finalizados
 * @param atrasados      planos em aberto com prazo vencido
 */
public record IndicadoresPlanoAcaoResposta(
        Map<String, Long> totalPorStatus,
        long total,
        long emAberto,
        long atrasados
) {
}
