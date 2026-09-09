package br.com.totvs.insight360.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Usuário da plataforma — responsável por planos de ação e autor de comentários.
 * <p>
 * A senha nunca é armazenada em texto puro: o serviço grava apenas o hash BCrypt.
 */
@Entity
@Table(name = "usuarios",
       uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"id", "nome", "email", "perfil", "ativo"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    /** Hash BCrypt da senha — nunca exposto em DTOs de resposta. */
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private Boolean ativo = Boolean.TRUE;

    @Column(length = 30)
    private String cargo;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    void aoInserir() {
        LocalDateTime agora = LocalDateTime.now();
        this.dataCadastro = agora;
        this.dataAtualizacao = agora;
        if (this.ativo == null) {
            this.ativo = Boolean.TRUE;
        }
    }

    @PreUpdate
    void aoAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    /** Conveniência para as regras de negócio: trata {@code null} como ativo. */
    public boolean isAtivo() {
        return !Boolean.FALSE.equals(ativo);
    }

    /** Verifica se este usuário pode editar/remover um registro criado por outro. */
    public boolean podeAlterarRegistroDe(Usuario outroUsuario) {
        if (outroUsuario == null) {
            return false;
        }
        if (Objects.equals(this.id, outroUsuario.getId())) {
            return true;
        }
        return perfil != null && perfil.podeAdministrarRegistrosDeTerceiros();
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Usuario outro)) {
            return false;
        }
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
