package br.com.totvs.hermes.service;

import br.com.totvs.hermes.model.Reuniao;

import java.util.List;
import java.util.Map;

/**
 * DTO imutável com todos os dados agregados do dashboard executivo.
 */
public record DadosDashboard(
        long totalReunioes,
        long totalClientes,
        long totalChurnAlto,
        long totalChurnMedio,
        long totalOportunidades,
        String scoreQualidadeMedia,
        String scoreComercialMedia,
        Map<String, Long> sentimentos,
        String sentimentoPredominante,
        Map<String, Long> topProdutos,
        Map<String, Long> topConcorrentes,
        Map<String, Long> topCategorias,
        List<Reuniao> topOportunidades,
        List<Reuniao> topChurn
) {}