package br.com.totvs.hermes.chat.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Provedores de LLM em ordem de prioridade ({@code hermes.llm.provedores[n].*}).
 * Todos falam o formato OpenAI {@code /chat/completions}.
 */
@ConfigurationProperties(prefix = "hermes.llm")
public record HermesLlmPropriedades(List<Provedor> provedores) {

    public HermesLlmPropriedades {
        provedores = provedores == null ? List.of() : List.copyOf(provedores);
    }

    /**
     * @param nome   apelido usado só no log (openrouter, groq, ollama...)
     * @param url    endpoint completo de chat/completions
     * @param chave  chave da API — vem de variável de ambiente; vazia = provedor ignorado
     * @param modelo identificador do modelo no provedor
     */
    public record Provedor(String nome, String url, String chave, String modelo) {
    }
}
