package br.com.totvs.insight360.model;

/**
 * Perfil de acesso de um {@link Usuario} da plataforma.
 * <p>
 * Cada perfil define o que o usuário pode fazer sobre os dados de acompanhamento
 * comercial (planos de ação, comentários e cadastros).
 */
public enum PerfilUsuario {

    /** Administrador: gerencia usuários e pode editar/remover qualquer registro. */
    ADMIN("Administrador"),

    /** Gestor comercial: acompanha a equipe e distribui planos de ação. */
    GESTOR("Gestor Comercial"),

    /** Vendedor: responsável pelas reuniões e pelos planos de ação atribuídos a ele. */
    VENDEDOR("Vendedor"),

    /** Analista: consulta as análises e registra comentários. */
    ANALISTA("Analista de Negócios");

    private final String descricao;

    PerfilUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Somente o administrador cria, inativa ou remove outros usuários. */
    public boolean podeGerenciarUsuarios() {
        return this == ADMIN;
    }

    /** Administrador e gestor podem editar/remover registros de terceiros. */
    public boolean podeAdministrarRegistrosDeTerceiros() {
        return this == ADMIN || this == GESTOR;
    }
}
