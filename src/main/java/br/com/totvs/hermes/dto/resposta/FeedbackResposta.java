package br.com.totvs.hermes.dto.resposta;

import br.com.totvs.hermes.model.NivelCriticidade;

/**
 * Feedback educativo gerado para uma reunião.
 */
public record FeedbackResposta(
        Long id,
        Long reuniaoId,
        String problemaIdentificado,
        String categoriaProblema,
        String motivoNaoIdentificadoAntes,
        String sinaisNaConversa,
        String comoIdentificarAntes,
        String perguntasRecomendadas,
        String acaoDeMelhoria,
        NivelCriticidade nivelCriticidade,
        String mensagemEducativa
) {
}
