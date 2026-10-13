package br.com.totvs.hermes.service;

import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.model.RiscoChurn;
import br.com.totvs.hermes.repository.IndicadoresReuniao;
import br.com.totvs.hermes.repository.ReuniaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_LIMIT = 5;

    private final ReuniaoRepository reuniaoRepository;

    /**
     * Os indicadores saem de uma projeção sem CLOBs; só as reuniões dos rankings
     * (no máximo 2 x {@value #TOP_LIMIT}) são carregadas como entidade completa.
     */
    public DadosDashboard getDados() {
        List<IndicadoresReuniao> todas = reuniaoRepository.findIndicadoresAnalisadas();

        long totalReunioes  = todas.size();
        long totalClientes  = todas.stream()
                .map(IndicadoresReuniao::cliente).filter(Objects::nonNull)
                .collect(Collectors.toSet()).size();
        long totalChurnAlto  = todas.stream()
                .filter(r -> RiscoChurn.ALTO == r.riscoChurn()).count();
        long totalChurnMedio = todas.stream()
                .filter(r -> RiscoChurn.MEDIO == r.riscoChurn()).count();
        long totalOportunidades = todas.stream()
                .filter(r -> r.temOportunidade() != null && r.temOportunidade() == 1).count();

        OptionalDouble avgQ = todas.stream()
                .mapToInt(r -> r.scoreQualidade() != null ? r.scoreQualidade() : 0).average();
        OptionalDouble avgC = todas.stream()
                .mapToInt(r -> r.scoreComercial() != null ? r.scoreComercial() : 0).average();
        String scoreQualidadeMedia = avgQ.isPresent() ? String.format("%.0f", avgQ.getAsDouble()) : "0";
        String scoreComercialMedia = avgC.isPresent() ? String.format("%.0f", avgC.getAsDouble()) : "0";

        Map<String, Long> sentimentos = todas.stream()
                .filter(r -> r.sentimento() != null)
                .collect(Collectors.groupingBy(r -> r.sentimento().name(), Collectors.counting()));
        String sentimentoPredominante = sentimentos.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");

        Map<String, Long> produtos     = contarItens(todas, IndicadoresReuniao::produtosIdentificados, ",");
        Map<String, Long> concorrentes = contarItens(todas, IndicadoresReuniao::concorrentesIdentificados, ",");
        Map<String, Long> categorias   = contarItens(todas, IndicadoresReuniao::categoriasPrincipais, ";");

        List<Long> idsTopOportunidades = todas.stream()
                .filter(r -> r.scoreComercial() != null && r.scoreComercial() > 0)
                .sorted(Comparator.comparingInt(IndicadoresReuniao::scoreComercial).reversed())
                .limit(TOP_LIMIT).map(IndicadoresReuniao::id).toList();

        List<Long> idsTopChurn = todas.stream()
                .filter(r -> RiscoChurn.ALTO == r.riscoChurn() || RiscoChurn.MEDIO == r.riscoChurn())
                .sorted(Comparator.comparingInt(r -> RiscoChurn.ALTO == r.riscoChurn() ? 0 : 1))
                .limit(TOP_LIMIT).map(IndicadoresReuniao::id).toList();

        Map<Long, Reuniao> topPorId = carregarPorId(idsTopOportunidades, idsTopChurn);

        return new DadosDashboard(
                totalReunioes, totalClientes, totalChurnAlto, totalChurnMedio,
                totalOportunidades, scoreQualidadeMedia, scoreComercialMedia,
                sentimentos, sentimentoPredominante,
                sortLimit(produtos), sortLimit(concorrentes), sortLimit(categorias),
                idsTopOportunidades.stream().map(topPorId::get).toList(),
                idsTopChurn.stream().map(topPorId::get).toList()
        );
    }

    /** Conta cada item de uma coluna com valores separados por {@code separador}. */
    private Map<String, Long> contarItens(List<IndicadoresReuniao> todas,
                                          Function<IndicadoresReuniao, String> coluna,
                                          String separador) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (IndicadoresReuniao r : todas) {
            String valor = coluna.apply(r);
            if (valor != null && !valor.isBlank())
                for (String parte : valor.split(separador)) {
                    String item = parte.trim();
                    if (!item.isBlank()) contagem.merge(item, 1L, Long::sum);
                }
        }
        return contagem;
    }

    /** Busca as reuniões dos rankings numa única consulta. */
    private Map<Long, Reuniao> carregarPorId(List<Long> ids1, List<Long> ids2) {
        Set<Long> ids = new LinkedHashSet<>(ids1);
        ids.addAll(ids2);
        if (ids.isEmpty()) return Map.of();
        return reuniaoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Reuniao::getId, Function.identity()));
    }

    private Map<String, Long> sortLimit(Map<String, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_LIMIT)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }
}
