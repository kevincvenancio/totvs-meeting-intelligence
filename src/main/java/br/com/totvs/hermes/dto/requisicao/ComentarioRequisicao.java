package br.com.totvs.hermes.dto.requisicao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Novo comentário de um usuário sobre uma reunião.
 */
public record ComentarioRequisicao(

        @NotNull(message = "O id do autor é obrigatório.")
        @Positive(message = "O id do autor deve ser positivo.")
        Long autorId,

        @NotBlank(message = "O texto do comentário é obrigatório.")
        @Size(min = 3, max = 2000, message = "O comentário deve ter entre 3 e 2000 caracteres.")
        String texto
) {
}
