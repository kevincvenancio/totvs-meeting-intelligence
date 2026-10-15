package br.com.totvs.hermes.dto.resposta;

import java.util.List;

/**
 * Resposta do Hermes Chat.
 *
 * @param resposta     texto para exibir ao usuário (markdown simples)
 * @param fontes       registros do sistema em que a resposta se apoiou
 * @param foraDoEscopo a pergunta foi recusada por estar fora do produto
 * @param meta         dados para a linha de rastreio do front (o que foi consultado, tempo, tamanho)
 */
public record ChatResposta(String resposta, List<Fonte> fontes, boolean foraDoEscopo, Meta meta) {

    /**
     * @param tipo {@code REUNIAO}, {@code CLIENTE} ou {@code PLANO_ACAO}
     * @param id   identificador do registro
     */
    public record Fonte(String tipo, Long id) {
    }

    /**
     * @param agente              {@code REUNIOES} ou {@code SUPORTE}
     * @param reunioesConsultadas reuniões enviadas no contexto
     * @param planosConsultados   planos de ação enviados no contexto
     * @param tokensContexto      estimativa (caracteres ÷ 4) do contexto enviado ao modelo
     * @param duracaoMs           tempo total da resposta no servidor
     * @param chamouModelo        false quando a resposta veio de um filtro, sem gastar cota
     */
    public record Meta(String agente, int reunioesConsultadas, int planosConsultados,
                       int tokensContexto, long duracaoMs, boolean chamouModelo) {
    }

    public static ChatResposta fixa(String texto, boolean foraDoEscopo, String agente, long duracaoMs) {
        return new ChatResposta(texto, List.of(), foraDoEscopo,
                new Meta(agente, 0, 0, 0, duracaoMs, false));
    }
}
