package br.com.totvs.insight360.dto.resposta;

import br.com.totvs.insight360.model.StatusCliente;

import java.time.LocalDateTime;

/**
 * Representação completa de um cliente na API.
 *
 * @param totalReunioes quantidade de reuniões já vinculadas ao cliente
 */
public record ClienteResposta(
        Long id,
        String razaoSocial,
        String nomeFantasia,
        String nomeExibicao,
        String cnpj,
        String cnpjFormatado,
        String segmento,
        String uf,
        String cidade,
        String faixaFaturamento,
        Double notaNps,
        StatusCliente status,
        String statusDescricao,
        String contatoPrincipal,
        String emailContato,
        String telefoneContato,
        String observacoes,
        Long totalReunioes,
        LocalDateTime dataCadastro,
        LocalDateTime dataAtualizacao
) {
}
