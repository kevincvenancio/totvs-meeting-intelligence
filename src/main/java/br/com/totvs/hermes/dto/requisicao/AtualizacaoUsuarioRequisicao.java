package br.com.totvs.hermes.dto.requisicao;

import br.com.totvs.hermes.model.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados editáveis de um usuário já cadastrado. A senha é alterada por endpoint
 * próprio ({@link AlteracaoSenhaRequisicao}).
 */
public record AtualizacaoUsuarioRequisicao(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @NotNull(message = "O perfil é obrigatório.")
        PerfilUsuario perfil,

        @Size(max = 30, message = "O cargo deve ter no máximo 30 caracteres.")
        String cargo,

        @NotNull(message = "Informe se o usuário está ativo.")
        Boolean ativo
) {
}
