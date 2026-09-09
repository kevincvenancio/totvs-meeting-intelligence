package br.com.totvs.insight360.dto.mapper;

import br.com.totvs.insight360.dto.requisicao.ReuniaoRequisicao;
import br.com.totvs.insight360.dto.resposta.ClienteResumoResposta;
import br.com.totvs.insight360.dto.resposta.ReuniaoDetalheResposta;
import br.com.totvs.insight360.dto.resposta.ReuniaoReferenciaResposta;
import br.com.totvs.insight360.dto.resposta.ReuniaoResumoResposta;
import br.com.totvs.insight360.model.Cliente;
import br.com.totvs.insight360.model.ImportacaoLote;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.util.TextoUtils;
import org.hibernate.Hibernate;

import java.util.List;

/**
 * Conversões entre {@link Reuniao} e seus DTOs (padrão Mapper/Assembler).
 * <p>
 * As associações lazy (lote e cliente vinculado) são lidas apenas quando já
 * vieram carregadas pela consulta, o que mantém o mapeamento seguro mesmo com
 * {@code spring.jpa.open-in-view=false}.
 */
public final class ReuniaoMapper {

    private ReuniaoMapper() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    /** Aplica os dados informados pelo Front-End sobre a entidade. */
    public static void aplicar(ReuniaoRequisicao requisicao, Reuniao reuniao) {
        reuniao.setIdExterno(textoOuNulo(requisicao.idExterno()));
        reuniao.setData(requisicao.data());
        reuniao.setCliente(requisicao.cliente().trim());
        reuniao.setVendedor(textoOuNulo(requisicao.vendedor()));
        reuniao.setFormato(textoOuNulo(requisicao.formato()));
        reuniao.setDuracaoMinutos(requisicao.duracaoMinutos());
        reuniao.setDuracaoFormatada(formatarDuracao(requisicao.duracaoMinutos()));
        reuniao.setUf(requisicao.uf() != null && !requisicao.uf().isBlank()
                ? requisicao.uf().trim().toUpperCase() : null);
        reuniao.setSegmento(textoOuNulo(requisicao.segmento()));
        reuniao.setNomeUnidade(textoOuNulo(requisicao.nomeUnidade()));
        reuniao.setFaturamento(textoOuNulo(requisicao.faturamento()));
        reuniao.setNotaNps(requisicao.notaNps());
        reuniao.setTranscricaoOriginal(requisicao.transcricao());
        reuniao.setTranscricaoTratada(TextoUtils.tratarTexto(requisicao.transcricao()));
    }

    public static ReuniaoResumoResposta paraResumo(Reuniao reuniao) {
        return new ReuniaoResumoResposta(
                reuniao.getId(),
                reuniao.getIdExterno(),
                reuniao.getData(),
                reuniao.getCliente(),
                reuniao.getVendedor(),
                reuniao.getCategoriaPrincipal(),
                reuniao.getStatusCompletude(),
                reuniao.getSentimento(),
                reuniao.getSentimentoFormatado(),
                reuniao.getRiscoChurn(),
                reuniao.getRiscoChurnLabel(),
                reuniao.getPrioridade(),
                reuniao.getScoreComercial(),
                reuniao.getScoreQualidade(),
                reuniao.getDuracaoDisplay(),
                Boolean.TRUE.equals(reuniao.getAnalisada()),
                Boolean.TRUE.equals(reuniao.getDuplicada()),
                codigoLote(reuniao),
                clienteVinculado(reuniao));
    }

    public static ReuniaoReferenciaResposta paraReferencia(Reuniao reuniao) {
        if (reuniao == null) {
            return null;
        }
        return new ReuniaoReferenciaResposta(reuniao.getId(), reuniao.getIdExterno(),
                reuniao.getData(), reuniao.getCliente());
    }

    /**
     * Monta o detalhe completo da reunião.
     *
     * @param totalInsights    quantidade de insights extraídos
     * @param totalComentarios quantidade de comentários registrados
     * @param totalPlanosAcao  quantidade de planos de ação criados
     */
    public static ReuniaoDetalheResposta paraDetalhe(Reuniao reuniao, long totalInsights,
                                                     long totalComentarios, long totalPlanosAcao) {
        return new ReuniaoDetalheResposta(
                reuniao.getId(),
                reuniao.getIdExterno(),
                reuniao.getData(),
                reuniao.getCliente(),
                reuniao.getVendedor(),
                reuniao.getFormato(),
                reuniao.getDuracaoDisplay(),
                reuniao.getDuracaoMinutos(),
                reuniao.getUf(),
                reuniao.getCnae(),
                reuniao.getNomeUnidade(),
                reuniao.getSegmento(),
                reuniao.getFaturamento(),
                reuniao.getNotaNps(),
                clienteVinculado(reuniao),

                reuniao.getStatusCompletude(),
                reuniao.getPontuacaoCompletude(),
                reuniao.getMotivoIncompletude(),
                reuniao.getInsightParcial(),
                reuniao.getSentimento(),
                reuniao.getSentimentoFormatado(),
                reuniao.getSentimentoJustificativa(),
                reuniao.getRiscoChurn(),
                reuniao.getRiscoChurnLabel(),
                reuniao.getPrioridade(),
                reuniao.getCategoriaPrincipal(),
                reuniao.getCategoriasPrincipais(),
                reuniao.getScoreQualidade(),
                reuniao.getScoreComercial(),

                reuniao.getTemaReuniao(),
                reuniao.getResumoReuniao(),
                reuniao.getPontosPrincipais(),
                reuniao.getDoresIdentificadas(),
                reuniao.getOportunidades(),
                reuniao.getRecomendacaoFinal(),
                reuniao.getProdutosIdentificados(),
                reuniao.getConcorrentesIdentificados(),
                reuniao.getPersonasIdentificadas(),
                reuniao.getEmpresasIdentificadas(),
                reuniao.getAreasInternas(),
                reuniao.getBudgetIdentificado(),
                reuniao.getLocutoresIdentificados(),
                reuniao.getQuantidadePalavras(),
                reuniao.getQuantidadeLocutores(),
                reuniao.getTranscricaoTratada(),

                Boolean.TRUE.equals(reuniao.getAnalisada()),
                Boolean.TRUE.equals(reuniao.getDuplicada()),
                reuniao.getMotivoDuplicidade(),
                reuniao.getIdReuniaoOriginalDuplicada(),
                codigoLote(reuniao),
                reuniao.getNomeArquivoOrigem(),
                reuniao.getDataImportacao(),

                totalInsights,
                totalComentarios,
                totalPlanosAcao);
    }

    public static List<ReuniaoResumoResposta> paraLista(List<Reuniao> reunioes) {
        return reunioes.stream().map(ReuniaoMapper::paraResumo).toList();
    }

    // ── Apoio ─────────────────────────────────────────────────────────

    /** Lê o código do lote somente quando a associação já está carregada. */
    private static String codigoLote(Reuniao reuniao) {
        ImportacaoLote lote = reuniao.getLoteImportacao();
        if (lote == null || !Hibernate.isInitialized(lote)) {
            return null;
        }
        return lote.getCodigoLote();
    }

    /** Lê o cliente vinculado somente quando a associação já está carregada. */
    private static ClienteResumoResposta clienteVinculado(Reuniao reuniao) {
        Cliente cliente = reuniao.getClienteVinculado();
        if (cliente == null || !Hibernate.isInitialized(cliente)) {
            return null;
        }
        return ClienteMapper.paraResumo(cliente);
    }

    private static String formatarDuracao(Integer minutos) {
        if (minutos == null || minutos <= 0) {
            return null;
        }
        int horas = minutos / 60;
        int resto = minutos % 60;
        if (horas > 0 && resto > 0) {
            return horas + "h " + resto + "min";
        }
        return horas > 0 ? horas + "h" : resto + "min";
    }

    private static String textoOuNulo(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
