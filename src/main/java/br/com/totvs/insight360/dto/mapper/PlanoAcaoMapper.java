package br.com.totvs.insight360.dto.mapper;

import br.com.totvs.insight360.dto.resposta.PlanoAcaoResposta;
import br.com.totvs.insight360.model.PlanoAcao;
import br.com.totvs.insight360.model.StatusPlanoAcao;

import java.util.Comparator;
import java.util.List;

/**
 * Conversões entre {@link PlanoAcao} e seus DTOs (padrão Mapper/Assembler).
 */
public final class PlanoAcaoMapper {

    private PlanoAcaoMapper() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    public static PlanoAcaoResposta paraResposta(PlanoAcao plano) {
        return new PlanoAcaoResposta(
                plano.getId(),
                plano.getTitulo(),
                plano.getDescricao(),
                plano.getPrioridade(),
                plano.getPrioridade() != null ? plano.getPrioridade().getDescricao() : null,
                plano.getStatus(),
                plano.getStatus() != null ? plano.getStatus().getDescricao() : null,
                plano.getPrazo(),
                plano.isAtrasado(),
                plano.getDiasRestantes(),
                plano.getResultado(),
                UsuarioMapper.paraResumo(plano.getResponsavel()),
                ReuniaoMapper.paraReferencia(plano.getReuniao()),
                transicoes(plano.getStatus()),
                plano.getDataCriacao(),
                plano.getDataAtualizacao(),
                plano.getDataConclusao());
    }

    public static List<PlanoAcaoResposta> paraLista(List<PlanoAcao> planos) {
        return planos.stream().map(PlanoAcaoMapper::paraResposta).toList();
    }

    /** Transições possíveis a partir do status atual, em ordem estável. */
    private static List<StatusPlanoAcao> transicoes(StatusPlanoAcao status) {
        if (status == null) {
            return List.of();
        }
        return status.transicoesPermitidas().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
    }
}
