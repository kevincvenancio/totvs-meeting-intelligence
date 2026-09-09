package br.com.totvs.insight360.dto.filtro;

import br.com.totvs.insight360.model.PrioridadePlanoAcao;
import br.com.totvs.insight360.model.StatusPlanoAcao;

import java.time.LocalDate;
import java.util.Set;

/**
 * Critérios de pesquisa de planos de ação usados pela listagem paginada.
 * Todos os campos são opcionais.
 *
 * @param termo            texto livre pesquisado no título e na descrição
 * @param reuniaoId        id da reunião de origem
 * @param responsavelId    id do usuário responsável
 * @param status           filtro por status do plano
 * @param prioridade       filtro por prioridade
 * @param somenteAtrasados quando {@code true}, traz apenas planos em aberto com prazo vencido
 * @param prazoAte         prazo máximo (inclusivo)
 * @param ordenarPor       campo de ordenação (ver {@link #CAMPOS_ORDENACAO})
 * @param direcao          "asc" ou "desc"
 */
public record FiltroPlanoAcao(
        String termo,
        Long reuniaoId,
        Long responsavelId,
        StatusPlanoAcao status,
        PrioridadePlanoAcao prioridade,
        Boolean somenteAtrasados,
        LocalDate prazoAte,
        String ordenarPor,
        String direcao
) {

    /** Campos aceitos na ordenação — evita concatenar entrada do usuário no JPQL. */
    public static final Set<String> CAMPOS_ORDENACAO =
            Set.of("id", "prazo", "titulo", "dataCriacao", "status");

    private static final String CAMPO_ORDENACAO_PADRAO = "prazo";

    /** Filtro sem nenhum critério — retorna a listagem completa. */
    public static FiltroPlanoAcao vazio() {
        return new FiltroPlanoAcao(null, null, null, null, null, null, null, null, null);
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
        return "desc".equalsIgnoreCase(direcao) ? "DESC" : "ASC";
    }

    public boolean isSomenteAtrasados() {
        return Boolean.TRUE.equals(somenteAtrasados);
    }
}
