package br.com.totvs.insight360.model;

/**
 * Prioridade de execução de um {@link PlanoAcao}.
 * O peso é usado para ordenar as listas exibidas no Front-End.
 */
public enum PrioridadePlanoAcao {

    ALTA("Alta", 1, 3),
    MEDIA("Média", 2, 10),
    BAIXA("Baixa", 3, 30);

    private final String descricao;
    private final int peso;
    private final int prazoSugeridoEmDias;

    PrioridadePlanoAcao(String descricao, int peso, int prazoSugeridoEmDias) {
        this.descricao = descricao;
        this.peso = peso;
        this.prazoSugeridoEmDias = prazoSugeridoEmDias;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPeso() {
        return peso;
    }

    public int getPrazoSugeridoEmDias() {
        return prazoSugeridoEmDias;
    }
}
