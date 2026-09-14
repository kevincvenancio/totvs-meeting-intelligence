package br.com.totvs.hermes.dto.requisicao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca de senha do usuário: exige a senha atual para confirmar a identidade.
 */
public record AlteracaoSenhaRequisicao(

        @NotBlank(message = "A senha atual é obrigatória.")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 6, max = 60, message = "A nova senha deve ter entre 6 e 60 caracteres.")
        String novaSenha
) {
}
