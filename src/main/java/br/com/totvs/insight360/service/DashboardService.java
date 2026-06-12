package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.ReuniaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_LIMIT = 5;

    private final ReuniaoRepository reuniaoRepository;

    public DadosDashboard getDados() {
        List<Reuniao> todas = reuniaoRepository.findByAnalisadaTrue();

        long totalReunioes  = todas.size();
        long totalClientes  = todas.stream()
                .map(Reuniao::getCliente).filter(Objects::nonNull)
                .collect(Collectors.toSet()).size();
        long totalChurnAlto  = todas.stream()
                .filter(r -> r.getRiscoChurn() != null && br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn()).count();
        long totalChurnMedio = todas.stream()
                .filter(r -> r.getRiscoChurn() != null && br.com.totvs.insight360.model.RiscoChurn.MEDIO == r.getRiscoChurn()).count();
        long totalOportunidades = todas.stream()
                .filter(r -> r.getOportunidades() != null && !r.getOportunidades().isBlank()).count();

        OptionalDouble avgQ = todas.stream()
                .mapToInt(r -> r.getScoreQualidade() != null ? r.getScoreQualidade() : 0).average();
        OptionalDouble avgC = todas.stream()
                .mapToInt(r -> r.getScoreComercial() != null ? r.getScoreComercial() : 0).average();
        String scoreQualidadeMedia = avgQ.isPresent() ? String.format("%.0f", avgQ.getAsDouble()) : "0";
        String scoreComercialMedia = avgC.isPresent() ? String.format("%.0f", avgC.getAsDouble()) : "0";

        Map<String, Long> sentimentos = todas.stream()
                .filter(r -> r.getSentimento() != null)
                .collect(Collectors.groupingBy(r -> r.getSentimento().name(), Collectors.counting()));
        String sentimentoPredominante = sentimentos.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");

        Map<String, Long> produtos = new LinkedHashMap<>();
        for (Reuniao r : todas) {
            if (r.getProdutosIdentificados() != null && !r.getProdutosIdentificados().isBlank())
                for (String p : r.getProdutosIdentificados().split(",")) {
                    String prod = p.trim();
                    if (!prod.isBlank()) produtos.merge(prod, 1L, Long::sum);
                }
        }

        Map<String, Long> concorrentes = new LinkedHashMap<>();
        for (Reuniao r : todas) {
            if (r.getConcorrentesIdentificados() != null && !r.getConcorrentesIdentificados().isBlank())
                for (String c : r.getConcorrentesIdentificados().split(",")) {
                    String item = c.trim();
                    if (!item.isBlank()) concorrentes.merge(item, 1L, Long::sum);
                }
        }

        Map<String, Long> categorias = new LinkedHashMap<>();
        for (Reuniao r : todas) {
            if (r.getCategoriasPrincipais() != null && !r.getCategoriasPrincipais().isBlank())
                for (String c : r.getCategoriasPrincipais().split(";")) {
                    String cat = c.trim();
                    if (!cat.isBlank()) categorias.merge(cat, 1L, Long::sum);
                }
        }

        List<Reuniao> topOportunidades = todas.stream()
                .filter(r -> r.getScoreComercial() != null && r.getScoreComercial() > 0)
                .sorted(Comparator.comparingInt(Reuniao::getScoreComercial).reversed())
                .limit(TOP_LIMIT).collect(Collectors.toList());

        List<Reuniao> topChurn = todas.stream()
                .filter(r -> r.getRiscoChurn() != null &&
                        (br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn() || br.com.totvs.insight360.model.RiscoChurn.MEDIO == r.getRiscoChurn()))
                .sorted(Comparator.comparingInt(r -> br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn() ? 0 : 1))
                .limit(TOP_LIMIT).collect(Collectors.toList());

        return new DadosDashboard(
                totalReunioes, totalClientes, totalChurnAlto, totalChurnMedio,
                totalOportunidades, scoreQualidadeMedia, scoreComercialMedia,
                sentimentos, sentimentoPredominante,
                sortLimit(produtos), sortLimit(concorrentes), sortLimit(categorias),
                topOportunidades, topChurn
        );
    }

    private Map<String, Long> sortLimit(Map<String, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_LIMIT)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }
}
