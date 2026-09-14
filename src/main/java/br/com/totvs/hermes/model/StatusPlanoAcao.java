package br.com.totvs.hermes.model;

import java.util.Set;

/**
 * Ciclo de vida de um {@link PlanoAcao}.
 * <p>
 * O próprio enum conhece as transições válidas (padrão <i>State</i> aplicado ao
 * domínio), de modo que a regra fica em um único lugar e não espalhada pelos
 * serviços ou pelos controllers.
 *
 * <pre>
 *   PENDENTE ──▶ EM_ANDAMENTO ──▶ CONCLUIDO
 *      │              │
 *      └──────────────┴────────▶ CANCELADO
 * </pre>
 */
public enum StatusPlanoAcao {

    PENDENTE("Pendente"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPlanoAcao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Estados terminais não admitem nova transição nem edição do plano. */
    public boolean isFinal() {
        return this == CONCLUIDO || this == CANCELADO;
    }

    /** Estados de trabalho contam para o cálculo de atraso. */
    public boolean isEmAberto() {
        return !isFinal();
    }

    /**
     * Indica se a transição para o status informado é permitida.
     *
     * @param novoStatus status de destino
     * @return {@code true} quando a transição é válida
     */
    public boolean podeTransicionarPara(StatusPlanoAcao novoStatus) {
        if (novoStatus == null || novoStatus == this) {
            return false;
        }
        return transicoesPermitidas().contains(novoStatus);
    }

    /** Conjunto de status alcançáveis a partir do status atual. */
    public Set<StatusPlanoAcao> transicoesPermitidas() {
        return switch (this) {
            case PENDENTE     -> Set.of(EM_ANDAMENTO, CONCLUIDO, CANCELADO);
            case EM_ANDAMENTO -> Set.of(PENDENTE, CONCLUIDO, CANCELADO);
            case CONCLUIDO, CANCELADO -> Set.of();
        };
    }
}
