package br.com.totvs.insight360.dto.requisicao;

import br.com.totvs.insight360.model.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro de um novo usuário.
 */
public record UsuarioRequisicao(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 60, message = "A senha deve ter entre 6 e 60 caracteres.")
        String senha,

        @NotNull(message = "O perfil é obrigatório.")
        PerfilUsuario perfil,

        @Size(max = 30, message = "O cargo deve ter no máximo 30 caracteres.")
        String cargo
) {
}
