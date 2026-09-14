package br.com.totvs.hermes.dto.mapper;

import br.com.totvs.hermes.dto.resposta.FeedbackResposta;
import br.com.totvs.hermes.dto.resposta.InsightResposta;
import br.com.totvs.hermes.model.FeedbackReuniao;
import br.com.totvs.hermes.model.Insight;

import java.util.List;

/**
 * Conversões dos resultados da análise automática ({@link Insight} e
 * {@link FeedbackReuniao}) para os DTOs da API.
 */
public final class AnaliseMapper {

    private AnaliseMapper() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    public static InsightResposta paraResposta(Insight insight) {
        return new InsightResposta(
                insight.getId(),
                insight.getReuniao() != null ? insight.getReuniao().getId() : null,
                insight.getTipo(),
                insight.getDescricao(),
                insight.getPrioridade(),
                insight.getTrechoOrigem(),
                insight.getConfianca());
    }

    public static List<InsightResposta> paraListaDeInsights(List<Insight> insights) {
        return insights.stream().map(AnaliseMapper::paraResposta).toList();
    }

    public static FeedbackResposta paraResposta(FeedbackReuniao feedback) {
        return new FeedbackResposta(
                feedback.getId(),
                feedback.getReuniao() != null ? feedback.getReuniao().getId() : null,
                feedback.getProblemaIdentificado(),
                feedback.getCategoriaProblema(),
                feedback.getMotivoNaoIdentificadoAntes(),
                feedback.getSinaisNaConversa(),
                feedback.getComoIdentificarAntes(),
                feedback.getPerguntasRecomendadas(),
                feedback.getAcaoDeMelhoria(),
                feedback.getNivelCriticidade(),
                feedback.getMensagemEducativa());
    }

    public static List<FeedbackResposta> paraListaDeFeedbacks(List<FeedbackReuniao> feedbacks) {
        return feedbacks.stream().map(AnaliseMapper::paraResposta).toList();
    }
}
