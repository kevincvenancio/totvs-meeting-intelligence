package br.com.totvs.insight360.dto.requisicao;

import br.com.totvs.insight360.model.PrioridadePlanoAcao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dados editáveis de um plano de ação. A reunião de origem é imutável e o status
 * é alterado pelo endpoint de transição ({@link TransicaoStatusRequisicao}).
 */
public record AtualizacaoPlanoAcaoRequisicao(

        @NotBlank(message = "O título é obrigatório.")
        @Size(min = 5, max = 120, message = "O título deve ter entre 5 e 120 caracteres.")
        String titulo,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "O id do responsável é obrigatório.")
        @Positive(message = "O id do responsável deve ser positivo.")
        Long responsavelId,

        @NotNull(message = "A prioridade é obrigatória.")
        PrioridadePlanoAcao prioridade,

        @NotNull(message = "O prazo é obrigatório.")
        LocalDate prazo
) {
}
