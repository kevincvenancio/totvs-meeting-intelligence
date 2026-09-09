package br.com.totvs.insight360.service;

import br.com.totvs.insight360.dto.mapper.ReuniaoMapper;
import br.com.totvs.insight360.dto.resposta.DashboardResposta;
import br.com.totvs.insight360.dto.resposta.IndicadoresClienteResposta;
import br.com.totvs.insight360.dto.resposta.IndicadoresPlanoAcaoResposta;
import br.com.totvs.insight360.model.StatusCliente;
import br.com.totvs.insight360.model.StatusPlanoAcao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Monta o painel executivo consumido pelo Front-End.
 * <p>
 * Atua como fachada (padrão <i>Facade</i>) sobre os indicadores já calculados
 * pelo {@link DashboardService} — reaproveitado pela CLI e pelos relatórios em
 * PDF — somando os indicadores de acompanhamento comercial (planos de ação e
 * carteira de clientes).
 */
@Service
@RequiredArgsConstructor
public class PainelExecutivoService {

    private final DashboardService dashboardService;
    private final PlanoAcaoService planoAcaoService;
    private final ClienteService clienteService;

    /** Consolida todos os indicadores do dashboard em um único DTO. */
    @Transactional(readOnly = true)
    public DashboardResposta montarPainel() {
        DadosDashboard dados = dashboardService.getDados();
        return new DashboardResposta(
                dados.totalReunioes(),
                dados.totalClientes(),
                dados.totalChurnAlto(),
                dados.totalChurnMedio(),
                dados.totalOportunidades(),
                dados.scoreQualidadeMedia(),
                dados.scoreComercialMedia(),
                dados.sentimentos(),
                dados.sentimentoPredominante(),
                dados.topProdutos(),
                dados.topConcorrentes(),
                dados.topCategorias(),
                ReuniaoMapper.paraLista(dados.topOportunidades()),
                ReuniaoMapper.paraLista(dados.topChurn()),
                montarIndicadoresDePlanos(),
                montarIndicadoresDeClientes());
    }

    private IndicadoresPlanoAcaoResposta montarIndicadoresDePlanos() {
        Map<String, Long> totalPorStatus = planoAcaoService.contarPorStatus();
        long total = totalPorStatus.values().stream().mapToLong(Long::longValue).sum();
        long emAberto = totalPorStatus.getOrDefault(StatusPlanoAcao.PENDENTE.name(), 0L)
                + totalPorStatus.getOrDefault(StatusPlanoAcao.EM_ANDAMENTO.name(), 0L);
        return new IndicadoresPlanoAcaoResposta(totalPorStatus, total, emAberto,
                planoAcaoService.contarAtrasados());
    }

    private IndicadoresClienteResposta montarIndicadoresDeClientes() {
        Map<StatusCliente, Long> porStatus = clienteService.contarPorStatus();
        Map<String, Long> totalPorStatus = new LinkedHashMap<>();
        porStatus.forEach((status, total) -> totalPorStatus.put(status.name(), total));
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        return new IndicadoresClienteResposta(totalPorStatus, total,
                porStatus.getOrDefault(StatusCliente.EM_RISCO, 0L));
    }
}
