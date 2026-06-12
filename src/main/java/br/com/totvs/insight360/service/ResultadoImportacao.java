package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.ImportacaoLote;

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
