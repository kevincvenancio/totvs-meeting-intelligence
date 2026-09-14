package br.com.totvs.hermes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Plano de ação criado a partir de uma reunião analisada.
 * <p>
 * É o elo entre a inteligência gerada pelo sistema (dores, oportunidades e risco
 * de churn) e a execução comercial: cada plano tem um responsável, um prazo e um
 * ciclo de vida controlado por {@link StatusPlanoAcao}.
 */
@Entity
@Table(name = "planos_acao")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"id", "titulo", "status", "prioridade", "prazo"})
public class PlanoAcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    /** Pesquisavel pelo Front-End: VARCHAR2 em vez de CLOB, no limite da validacao. */
    @Column(length = 2000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reuniao_id", nullable = false)
    private Reuniao reuniao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadePlanoAcao prioridade = PrioridadePlanoAcao.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPlanoAcao status = StatusPlanoAcao.PENDENTE;

    @Column(nullable = false)
    private LocalDate prazo;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    /** Descrição do desfecho — obrigatória ao concluir ou cancelar o plano. */
    @Lob
    private String resultado;

    @PrePersist
    void aoInserir() {
        LocalDateTime agora = LocalDateTime.now();
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
        if (this.status == null) {
            this.status = StatusPlanoAcao.PENDENTE;
        }
        if (this.prioridade == null) {
            this.prioridade = PrioridadePlanoAcao.MEDIA;
        }
    }

    @PreUpdate
    void aoAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    /** Plano em aberto cujo prazo já venceu. */
    public boolean isAtrasado() {
        return status != null && status.isEmAberto()
                && prazo != null && prazo.isBefore(LocalDate.now());
    }

    /**
     * Dias restantes até o prazo (negativo quando atrasado).
     * Retorna {@code null} para planos já finalizados.
     */
    public Long getDiasRestantes() {
        if (prazo == null || status == null || status.isFinal()) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), prazo);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof PlanoAcao outro)) {
            return false;
        }
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
