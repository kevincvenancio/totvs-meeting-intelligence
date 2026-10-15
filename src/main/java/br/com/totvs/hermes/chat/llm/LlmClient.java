package br.com.totvs.hermes.chat.llm;

import java.util.List;

/**
 * Contrato mínimo de um modelo de linguagem: recebe a conversa e devolve o texto da resposta.
 */
public interface LlmClient {

    /**
     * @throws ProvedorLlmException quando o provedor não conseguiu responder
     */
    String completar(List<Mensagem> mensagens);
}
