package br.com.totvs.hermes.dto.resposta;

import br.com.totvs.hermes.model.StatusCliente;

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
