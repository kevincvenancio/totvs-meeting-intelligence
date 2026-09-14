package br.com.totvs.hermes.dto.requisicao;

import br.com.totvs.hermes.model.StatusCliente;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro e edição de um cliente.
 * O CNPJ pode ser enviado formatado; o serviço remove a máscara e valida os
 * dígitos verificadores antes de gravar.
 */
public record ClienteRequisicao(

        @NotBlank(message = "A razão social é obrigatória.")
        @Size(min = 3, max = 150, message = "A razão social deve ter entre 3 e 150 caracteres.")
        String razaoSocial,

        @Size(max = 150, message = "O nome fantasia deve ter no máximo 150 caracteres.")
        String nomeFantasia,

        @NotBlank(message = "O CNPJ é obrigatório.")
        @Pattern(regexp = "\\D*(\\d\\D*){14}", message = "O CNPJ deve conter 14 dígitos.")
        String cnpj,

        @Size(max = 80, message = "O segmento deve ter no máximo 80 caracteres.")
        String segmento,

        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "A UF deve ter 2 letras.")
        String uf,

        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres.")
        String cidade,

        @Size(max = 60, message = "A faixa de faturamento deve ter no máximo 60 caracteres.")
        String faixaFaturamento,

        @DecimalMin(value = "0.0", message = "A nota NPS mínima é 0.")
        @DecimalMax(value = "10.0", message = "A nota NPS máxima é 10.")
        Double notaNps,

        StatusCliente status,

        @Size(max = 120, message = "O contato principal deve ter no máximo 120 caracteres.")
        String contatoPrincipal,

        @Email(message = "Informe um e-mail de contato válido.")
        @Size(max = 150, message = "O e-mail de contato deve ter no máximo 150 caracteres.")
        String emailContato,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String telefoneContato,

        String observacoes
) {
}
