package br.com.totvs.hermes.chat.llm;

/**
 * Um provedor concreto dentro da cadeia de fallback.
 */
public interface ProvedorLlm extends LlmClient {

    /** Apelido para log. Nunca é exibido ao usuário. */
    String nome();

    /** Provedor sem chave configurada fica fora da cadeia. */
    boolean configurado();
}
