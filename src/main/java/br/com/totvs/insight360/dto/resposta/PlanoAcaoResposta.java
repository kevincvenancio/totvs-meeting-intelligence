package br.com.totvs.insight360.dto.resposta;

import br.com.totvs.insight360.model.PrioridadePlanoAcao;
import br.com.totvs.insight360.model.StatusPlanoAcao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Plano de ação em formato de API.
 *
 * @param transicoesPermitidas status para os quais o plano pode ser movido —
 *                             permite ao Front-End habilitar apenas os botões válidos
 */
public record PlanoAcaoResposta(
        Long id,
        String titulo,
        String descricao,
        PrioridadePlanoAcao prioridade,
        String prioridadeDescricao,
        StatusPlanoAcao status,
        String statusDescricao,
        LocalDate prazo,
        boolean atrasado,
        Long diasRestantes,
        String resultado,
        UsuarioResumoResposta responsavel,
        ReuniaoReferenciaResposta reuniao,
        List<StatusPlanoAcao> transicoesPermitidas,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        LocalDateTime dataConclusao
) {
}
