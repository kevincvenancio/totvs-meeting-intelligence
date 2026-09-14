package br.com.totvs.hermes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Cliente TOTVS cadastrado na base de acompanhamento comercial.
 * <p>
 * As reuniões importadas do CSV trazem apenas o nome da unidade em texto livre;
 * esta entidade normaliza esse dado e permite vincular várias reuniões ao mesmo
 * cliente (ver {@link Reuniao#getClienteVinculado()}).
 */
@Entity
@Table(name = "clientes",
       uniqueConstraints = @UniqueConstraint(name = "uk_cliente_cnpj", columnNames = "cnpj"))
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"id", "razaoSocial", "cnpj", "status"})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 150)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    /** CNPJ armazenado somente com dígitos (14 posições). */
    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(length = 80)
    private String segmento;

    @Column(length = 2)
    private String uf;

    @Column(length = 80)
    private String cidade;

    @Column(name = "faixa_faturamento", length = 60)
    private String faixaFaturamento;

    @Column(name = "nota_nps")
    private Double notaNps;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCliente status = StatusCliente.PROSPECT;

    @Column(name = "contato_principal", length = 120)
    private String contatoPrincipal;

    @Column(name = "email_contato", length = 150)
    private String emailContato;

    @Column(name = "telefone_contato", length = 20)
    private String telefoneContato;

    @Lob
    private String observacoes;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    void aoInserir() {
        LocalDateTime agora = LocalDateTime.now();
        this.dataCadastro = agora;
        this.dataAtualizacao = agora;
        if (this.status == null) {
            this.status = StatusCliente.PROSPECT;
        }
    }

    @PreUpdate
    void aoAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    /** Nome preferencial para exibição no Front-End. */
    public String getNomeExibicao() {
        return nomeFantasia != null && !nomeFantasia.isBlank() ? nomeFantasia : razaoSocial;
    }

    /** CNPJ formatado como 00.000.000/0000-00. */
    public String getCnpjFormatado() {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + '.' + cnpj.substring(2, 5) + '.' + cnpj.substring(5, 8)
                + '/' + cnpj.substring(8, 12) + '-' + cnpj.substring(12);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Cliente outro)) {
            return false;
        }
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
