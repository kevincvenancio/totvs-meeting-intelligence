package br.com.totvs.insight360.dto.filtro;

import br.com.totvs.insight360.model.RiscoChurn;
import br.com.totvs.insight360.model.Sentimento;
import br.com.totvs.insight360.model.StatusCompletude;

import java.time.LocalDate;
import java.util.Set;

/**
 * Critérios de pesquisa de reuniões usados pela listagem paginada do Front-End.
 * <p>
 * Todos os campos são opcionais: os nulos simplesmente não entram na consulta.
 *
 * @param termo             texto livre pesquisado em cliente, tema e resumo
 * @param clienteId         id do cliente cadastrado vinculado à reunião
 * @param loteId            id do lote de importação
 * @param statusCompletude  filtro por completude da transcrição
 * @param sentimento        filtro por sentimento identificado
 * @param riscoChurn        filtro por risco de churn
 * @param prioridade        filtro por prioridade comercial (ALTA / MEDIA / BAIXA)
 * @param dataInicial       data mínima da reunião (inclusiva)
 * @param dataFinal         data máxima da reunião (inclusiva)
 * @param somenteAnalisadas quando {@code true}, traz apenas reuniões já analisadas
 * @param incluirDuplicadas quando {@code true}, inclui as reuniões marcadas como duplicadas
 * @param ordenarPor        campo de ordenação (ver {@link #CAMPOS_ORDENACAO})
 * @param direcao           "asc" ou "desc"
 */
public record FiltroReuniao(
        String termo,
        Long clienteId,
        Long loteId,
        StatusCompletude statusCompletude,
        Sentimento sentimento,
        RiscoChurn riscoChurn,
        String prioridade,
        LocalDate dataInicial,
        LocalDate dataFinal,
        Boolean somenteAnalisadas,
        Boolean incluirDuplicadas,
        String ordenarPor,
        String direcao
) {

    /** Campos aceitos na ordenação — evita concatenar entrada do usuário no JPQL. */
    public static final Set<String> CAMPOS_ORDENACAO =
            Set.of("id", "data", "cliente", "scoreComercial", "scoreQualidade", "pontuacaoCompletude");

    private static final String CAMPO_ORDENACAO_PADRAO = "id";

    /** Filtro sem nenhum critério — retorna a listagem completa. */
    public static FiltroReuniao vazio() {
        return new FiltroReuniao(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    /** Campo de ordenação validado contra a lista branca. */
    public String campoOrdenacao() {
        if (ordenarPor != null && CAMPOS_ORDENACAO.contains(ordenarPor)) {
            return ordenarPor;
        }
        return CAMPO_ORDENACAO_PADRAO;
    }

    /** Direção de ordenação validada ("ASC" ou "DESC"). */
    public String direcaoOrdenacao() {
        return "asc".equalsIgnoreCase(direcao) ? "ASC" : "DESC";
    }

    public boolean isSomenteAnalisadas() {
        return Boolean.TRUE.equals(somenteAnalisadas);
    }

    public boolean isIncluirDuplicadas() {
        return Boolean.TRUE.equals(incluirDuplicadas);
    }
}
