package br.com.totvs.hermes.dto.requisicao;

import br.com.totvs.hermes.model.StatusPlanoAcao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Mudança de status de um plano de ação.
 * O campo {@code resultado} é obrigatório ao concluir ou cancelar — a validação
 * fica na camada de serviço, junto das demais regras de negócio.
 */
public record TransicaoStatusRequisicao(

        @NotNull(message = "O novo status é obrigatório.")
        StatusPlanoAcao novoStatus,

        @Size(max = 2000, message = "O resultado deve ter no máximo 2000 caracteres.")
        String resultado
) {
}
