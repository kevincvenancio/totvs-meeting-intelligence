package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Reuniao;

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