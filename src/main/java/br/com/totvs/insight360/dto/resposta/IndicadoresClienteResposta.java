package br.com.totvs.insight360.dto.resposta;

import java.util.Map;

/**
 * Indicadores da carteira de clientes exibidos no dashboard.
 *
 * @param totalPorStatus quantidade de clientes em cada situação
 * @param total          total de clientes cadastrados
 * @param emRisco        clientes marcados como EM_RISCO
 */
public record IndicadoresClienteResposta(
        Map<String, Long> totalPorStatus,
        long total,
        long emRisco
) {
}
