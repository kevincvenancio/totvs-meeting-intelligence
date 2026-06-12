package br.com.totvs.insight360.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa um lote de importação — criado a cada CSV importado.
 * Permite rastrear a origem de cada reunião e manter histórico separado.
 */
@Entity
@Table(name = "importacao_lotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportacaoLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoLote;

    private String nomeArquivoOriginal;

    @Column(nullable = false)
    private LocalDateTime dataHoraImportacao;

    private int totalRegistrosBrutos;

    private int totalReunioesValidas;

    private int totalReunioesIncompletas;

    private int totalReunioesDuplicadas;

    private int totalReunioesComErro;

    private String statusProcessamento;

    private String hashArquivo;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
