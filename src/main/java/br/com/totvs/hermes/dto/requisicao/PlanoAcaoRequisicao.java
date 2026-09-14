package br.com.totvs.hermes.dto.requisicao;

import br.com.totvs.hermes.model.PrioridadePlanoAcao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dados de criação de um plano de ação a partir de uma reunião.
 */
public record PlanoAcaoRequisicao(

        @NotBlank(message = "O título é obrigatório.")
        @Size(min = 5, max = 120, message = "O título deve ter entre 5 e 120 caracteres.")
        String titulo,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "O id da reunião é obrigatório.")
        @Positive(message = "O id da reunião deve ser positivo.")
        Long reuniaoId,

        @NotNull(message = "O id do responsável é obrigatório.")
        @Positive(message = "O id do responsável deve ser positivo.")
        Long responsavelId,

        PrioridadePlanoAcao prioridade,

        @NotNull(message = "O prazo é obrigatório.")
        LocalDate prazo
) {
}
