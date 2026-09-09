package br.com.totvs.insight360.dto.resposta;

import br.com.totvs.insight360.model.StatusCliente;

/**
 * Identificação resumida de um cliente, usada dentro de outras respostas.
 */
public record ClienteResumoResposta(
        Long id,
        String nomeExibicao,
        String cnpjFormatado,
        StatusCliente status
) {
}
