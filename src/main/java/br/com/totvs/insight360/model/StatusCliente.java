package br.com.totvs.insight360.model;

/**
 * Situação comercial de um {@link Cliente} na base.
 */
public enum StatusCliente {

    /** Ainda não é cliente — em prospecção. */
    PROSPECT("Prospect"),

    /** Contrato vigente e sem sinais relevantes de churn. */
    ATIVO("Ativo"),

    /** Contrato vigente, porém com risco de churn identificado nas reuniões. */
    EM_RISCO("Em risco"),

    /** Contrato encerrado. */
    INATIVO("Inativo");

    private final String descricao;

    StatusCliente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Clientes inativos não recebem novos planos de ação. */
    public boolean aceitaNovoPlanoDeAcao() {
        return this != INATIVO;
    }
}
