package br.com.totvs.insight360.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma reunião importada do CSV e analisada pelo sistema.
 * <p>
 * Status de completude:
 *   COMPLETA    — 80-100 pts: cliente, data, transcrição, dor e solução/encaminhamento presentes
 *   PARCIAL     — 45-79 pts: informações úteis mas faltam partes importantes
 *   INCOMPLETA  — 0-44 pts : transcrição quebrada, sem contexto ou impossível de analisar
 */
@Entity
@Table(name = "reunioes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reuniao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String  idExterno;
    private LocalDate data;
    private LocalDate dtUltimaPesquisa;
    private String  cliente;
    private String  vendedor;
    private String  tpRecurso;
    private Boolean flgExterno;
    private Integer duracaoMinutos;
    private String  duracaoFormatada;
    private String  formato;
    private String  statusMeetingOriginal;
    private String  uf;
    private String  cnae;
    private String  nomeUnidade;
    private String  segmento;
    private String  faturamento;
    private Double  notaNps;

    @Column(columnDefinition = "TEXT")
    private String transcricaoOriginal;

    @Column(columnDefinition = "TEXT")
    private String transcricaoTratada;

    @Enumerated(EnumType.STRING)
    private StatusCompletude statusCompletude;

    private Integer pontuacaoCompletude;

    @Column(columnDefinition = "TEXT")
    private String motivoIncompletude;

    @Column(columnDefinition = "TEXT")
    private String insightParcial;

    @Column(columnDefinition = "TEXT")
    private String locutoresIdentificados;

    @Enumerated(EnumType.STRING)
    private Sentimento sentimento;

    @Column(columnDefinition = "TEXT")
    private String sentimentoJustificativa;

    @Enumerated(EnumType.STRING)
    private RiscoChurn riscoChurn;

    private String budgetIdentificado;
    private String produtosIdentificados;
    private String concorrentesIdentificados;
    private String personasIdentificadas;
    private String empresasIdentificadas;
    private String areasInternas;
    private String categoriaReuniao;
    private String categoriasPrincipais;
    private String prioridade;

    @Column(columnDefinition = "TEXT")
    private String temaReuniao;

    @Column(columnDefinition = "TEXT")
    private String resumoReuniao;

    @Column(columnDefinition = "TEXT")
    private String pontosPrincipais;

    @Column(columnDefinition = "TEXT")
    private String doresIdentificadas;

    @Column(columnDefinition = "TEXT")
    private String oportunidades;

    @Column(columnDefinition = "TEXT")
    private String recomendacaoFinal;

    @Column(columnDefinition = "TEXT")
    private String insightsJson;

    @Column(columnDefinition = "TEXT")
    private String feedbackEducativo;

    private Integer scoreQualidade;
    private Integer scoreComercial;
    private Integer quantidadePalavras;
    private Integer quantidadeLocutores;
    private Boolean analisada = false;  // corrigido: cast (Boolean) removido

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private ImportacaoLote loteImportacao;

    private String nomeArquivoOrigem;
    private LocalDateTime dataImportacao;

    private String hashReuniao;
    private Boolean duplicada = false;  // corrigido: cast (Boolean) removido
    private String motivoDuplicidade;
    private Long idReuniaoOriginalDuplicada;

    public String getCategoriaPrincipal() {
        if (categoriaReuniao != null && !categoriaReuniao.isBlank()) return categoriaReuniao;
        if (categoriasPrincipais != null && !categoriasPrincipais.isBlank())
            return categoriasPrincipais.split(";")[0].trim();
        return "Não categorizado";
    }

    public String getRiscoChurnLabel() {
        if (riscoChurn == null) return "Baixo";
        return switch (riscoChurn) {
            case ALTO  -> "Alto";
            case MEDIO -> "Médio";
            default    -> "Baixo";
        };
    }

    public String getSentimentoFormatado() {
        if (sentimento == null) return "Neutro";
        return switch (sentimento) {
            case POSITIVO              -> "Positivo";
            case NEGATIVO              -> "Negativo";
            case MISTO                 -> "Misto";
            case CRITICO               -> "Crítico";
            case OPORTUNIDADE_COMERCIAL -> "Oportunidade Comercial";
            default                    -> "Neutro";
        };
    }

    public boolean isCompleta() {
        return StatusCompletude.COMPLETA == statusCompletude;
    }

    public boolean isParcial() {
        return StatusCompletude.PARCIAL == statusCompletude;
    }

    public boolean isIncompleta() {
        return StatusCompletude.INCOMPLETA == statusCompletude;
    }

    public String getDuracaoDisplay() {
        if (duracaoFormatada != null && !duracaoFormatada.isBlank()) return duracaoFormatada;
        if (duracaoMinutos != null && duracaoMinutos > 0) {
            int h = duracaoMinutos / 60;
            int m = duracaoMinutos % 60;
            return h > 0 ? h + "h " + m + "min" : m + "min";
        }
        return "Não informado";
    }
}