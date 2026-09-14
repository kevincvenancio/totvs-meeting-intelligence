package br.com.totvs.hermes.dto.requisicao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Vínculo de uma reunião importada a um cliente cadastrado.
 */
public record VinculoClienteRequisicao(

        @NotNull(message = "O id do cliente é obrigatório.")
        @Positive(message = "O id do cliente deve ser positivo.")
        Long clienteId
) {
}
