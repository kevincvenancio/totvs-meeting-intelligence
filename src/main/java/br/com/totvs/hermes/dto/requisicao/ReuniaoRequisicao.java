package br.com.totvs.hermes.dto.requisicao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cadastro ou edição manual de uma reunião — usado quando a transcrição não veio
 * pela importação de CSV. Após gravar, o serviço executa a mesma análise
 * automática aplicada às reuniões importadas.
 */
public record ReuniaoRequisicao(

        @Size(max = 40, message = "O identificador externo deve ter no máximo 40 caracteres.")
        String idExterno,

        @NotNull(message = "A data da reunião é obrigatória.")
        LocalDate data,

        @NotBlank(message = "O cliente é obrigatório.")
        @Size(min = 2, max = 150, message = "O cliente deve ter entre 2 e 150 caracteres.")
        String cliente,

        @Size(max = 120, message = "O vendedor deve ter no máximo 120 caracteres.")
        String vendedor,

        @Size(max = 60, message = "O formato deve ter no máximo 60 caracteres.")
        String formato,

        @PositiveOrZero(message = "A duração não pode ser negativa.")
        Integer duracaoMinutos,

        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "A UF deve ter 2 letras.")
        String uf,

        @Size(max = 80, message = "O segmento deve ter no máximo 80 caracteres.")
        String segmento,

        @Size(max = 150, message = "O nome da unidade deve ter no máximo 150 caracteres.")
        String nomeUnidade,

        @Size(max = 60, message = "A faixa de faturamento deve ter no máximo 60 caracteres.")
        String faturamento,

        @DecimalMin(value = "0.0", message = "A nota NPS mínima é 0.")
        @DecimalMax(value = "10.0", message = "A nota NPS máxima é 10.")
        Double notaNps,

        @NotBlank(message = "A transcrição é obrigatória.")
        @Size(min = 20, message = "A transcrição deve ter ao menos 20 caracteres.")
        String transcricao,

        @Positive(message = "O id do cliente vinculado deve ser positivo.")
        Long clienteId
) {
}
