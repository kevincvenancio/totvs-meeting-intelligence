package br.com.totvs.hermes.dto.requisicao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Edição de um comentário. O {@code usuarioId} identifica quem está editando —
 * apenas o autor, um gestor ou um administrador podem alterar o texto.
 */
public record AtualizacaoComentarioRequisicao(

        @NotNull(message = "O id do usuário é obrigatório.")
        @Positive(message = "O id do usuário deve ser positivo.")
        Long usuarioId,

        @NotBlank(message = "O texto do comentário é obrigatório.")
        @Size(min = 3, max = 2000, message = "O comentário deve ter entre 3 e 2000 caracteres.")
        String texto
) {
}
