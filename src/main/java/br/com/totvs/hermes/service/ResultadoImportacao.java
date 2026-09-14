package br.com.totvs.hermes.service;

import br.com.totvs.hermes.model.ImportacaoLote;

import java.util.List;

/**
 * DTO imutável com o resultado de uma importação de CSV.
 */
public record ResultadoImportacao(
    ImportacaoLote lote,
    int processadas,
    int incompletas,
    int duplicadas,
    int erros,
    int totalEncontradas,
    List<String> errosDetalhe
) {}
