package br.com.totvs.hermes.chat.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente para qualquer API no formato OpenAI {@code /chat/completions}
 * (OpenRouter, Groq, Cerebras, Ollama...). Usa o {@link RestClient} que já vem
 * com o Spring Boot 3.2 — nenhuma dependência nova.
 */
public class ProvedorOpenAiCompat implements ProvedorLlm {

    static final double TEMPERATURA = 0.2;
    /**
     * Teto de tokens gerados. Modelos de raciocínio (gpt-oss, qwen3) gastam parte disso
     * "pensando" antes de escrever; com 500 a resposta podia sair vazia. O tamanho final
     * da resposta continua limitado pelo pós-filtro (1.500 caracteres).
     */
    static final int MAX_TOKENS = 1500;

    private final String nome;
    private final String url;
    private final String chave;
    private final String modelo;
    private final RestClient http;

    public ProvedorOpenAiCompat(HermesLlmPropriedades.Provedor config, Duration timeout) {
        this.nome = config.nome() == null ? "provedor" : config.nome();
        this.url = config.url();
        this.chave = config.chave();
        this.modelo = config.modelo();

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) Math.min(timeout.toMillis(), 10_000));
        fabrica.setReadTimeout((int) timeout.toMillis());
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public String nome() {
        return nome;
    }

    @Override
    public boolean configurado() {
        return chave != null && !chave.isBlank() && url != null && !url.isBlank()
                && modelo != null && !modelo.isBlank();
    }

    @Override
    public String completar(List<Mensagem> mensagens) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", modelo);
        corpo.put("temperature", TEMPERATURA);
        corpo.put("max_tokens", MAX_TOKENS);
        if (modelo.contains("gpt-oss")) {
            // Pouco raciocínio: a resposta é curta e baseada só no contexto enviado
            corpo.put("reasoning_effort", "low");
        }
        corpo.put("messages", mensagens.stream()
                .map(m -> Map.of("role", m.papel(), "content", m.conteudo()))
                .toList());

        try {
            JsonNode resposta = http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + chave)
                    // Cabeçalhos opcionais do OpenRouter (identificam o app no painel deles)
                    .header("X-Title", "TOTVS Meeting Intelligence")
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);

            String texto = resposta == null ? null
                    : resposta.path("choices").path(0).path("message").path("content").asText(null);
            if (texto == null || texto.isBlank()) {
                throw new ProvedorLlmException(nome + ": resposta vazia", 502);
            }
            // Alguns modelos (qwen3, deepseek) devolvem o raciocínio entre <think> no texto
            String limpo = texto.replaceAll("(?s)<think>.*?</think>", "").trim();
            if (limpo.isBlank()) {
                throw new ProvedorLlmException(nome + ": resposta vazia", 502);
            }
            return limpo;

        } catch (RestClientResponseException e) {
            throw new ProvedorLlmException(nome + ": HTTP " + e.getStatusCode().value(),
                    e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new ProvedorLlmException(nome + ": timeout ou falha de rede", e);
        } catch (RestClientException e) {
            throw new ProvedorLlmException(nome + ": resposta inválida", e);
        }
    }
}
