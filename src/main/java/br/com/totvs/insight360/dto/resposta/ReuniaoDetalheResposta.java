package br.com.totvs.insight360.dto.resposta;

import br.com.totvs.insight360.model.RiscoChurn;
import br.com.totvs.insight360.model.Sentimento;
import br.com.totvs.insight360.model.StatusCompletude;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reunião em formato de detalhe — traz a análise completa, os dados cadastrais e
 * os contadores usados na tela de detalhe do Front-End.
 */
public record ReuniaoDetalheResposta(
        Long id,
        String idExterno,
        LocalDate data,
        String cliente,
        String vendedor,
        String formato,
        String duracao,
        Integer duracaoMinutos,
        String uf,
        String cnae,
        String nomeUnidade,
        String segmento,
        String faturamento,
        Double notaNps,
        ClienteResumoResposta clienteVinculado,

        // ── Classificação automática ──────────────────────────────────
        StatusCompletude statusCompletude,
        Integer pontuacaoCompletude,
        String motivoIncompletude,
        String insightParcial,
        Sentimento sentimento,
        String sentimentoFormatado,
        String sentimentoJustificativa,
        RiscoChurn riscoChurn,
        String riscoChurnLabel,
        String prioridade,
        String categoriaPrincipal,
        String categoriasPrincipais,
        Integer scoreQualidade,
        Integer scoreComercial,

        // ── Conteúdo analisado ────────────────────────────────────────
        String temaReuniao,
        String resumoReuniao,
        String pontosPrincipais,
        String doresIdentificadas,
        String oportunidades,
        String recomendacaoFinal,
        String produtosIdentificados,
        String concorrentesIdentificados,
        String personasIdentificadas,
        String empresasIdentificadas,
        String areasInternas,
        String budgetIdentificado,
        String locutoresIdentificados,
        Integer quantidadePalavras,
        Integer quantidadeLocutores,
        String transcricaoTratada,

        // ── Origem e situação ─────────────────────────────────────────
        boolean analisada,
        boolean duplicada,
        String motivoDuplicidade,
        Long idReuniaoOriginalDuplicada,
        String codigoLote,
        String nomeArquivoOrigem,
        LocalDateTime dataImportacao,

        // ── Contadores de apoio ───────────────────────────────────────
        long totalInsights,
        long totalComentarios,
        long totalPlanosAcao
) {
}
