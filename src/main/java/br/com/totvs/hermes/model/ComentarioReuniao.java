package br.com.totvs.hermes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Comentário registrado por um {@link Usuario} sobre uma {@link Reuniao}.
 * Permite que o time complemente a análise automática com contexto humano.
 */
@Entity
@Table(name = "comentarios_reuniao")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"id", "dataCriacao", "editado"})
public class ComentarioReuniao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reuniao_id", nullable = false)
    private Reuniao reuniao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Lob
    @Column(nullable = false)
    private String texto;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(nullable = false)
    private Boolean editado = Boolean.FALSE;

    @PrePersist
    void aoInserir() {
        LocalDateTime agora = LocalDateTime.now();
        this.dataCriacao = agora;
        this.dataAtualizacao = agora;
        if (this.editado == null) {
            this.editado = Boolean.FALSE;
        }
    }

    /** Registra a edição do texto mantendo a autoria original. */
    public void alterarTexto(String novoTexto) {
        this.texto = novoTexto;
        this.editado = Boolean.TRUE;
        this.dataAtualizacao = LocalDateTime.now();
    }

    public boolean isEditado() {
        return Boolean.TRUE.equals(editado);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ComentarioReuniao outro)) {
            return false;
        }
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
