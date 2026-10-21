package br.com.totvs.hermes.ia;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP do serviço de IA em Python ({@code ciencia-de-dados/api.py}).
 * <p>
 * O serviço roda o pipeline Transcrição → Processamento → Modelo → Indicadores →
 * Recomendação com dois modelos do Hugging Face (sentimento DistilBERT multilíngue e
 * zero-shot mDeBERTa-v3 XNLI) e devolve um JSON com produto, sentimento, churn,
 * oportunidade, termos e recomendações.
 */
@Component
@Slf4j
@EnableConfigurationProperties(HermesIaPropriedades.class)
public class HermesIaClient {

    private final HermesIaPropriedades propriedades;
    private final RestClient http;

    public HermesIaClient(HermesIaPropriedades propriedades) {
        this.propriedades = propriedades;
        Duration timeout = Duration.ofSeconds(propriedades.timeoutSegundos());
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout((int) timeout.toMillis());
        this.http = RestClient.builder().requestFactory(fabrica).build();
        if (propriedades.habilitado()) {
            log.info("IA Hermes habilitada em {} (modo {})", propriedades.url(), propriedades.modo());
        } else {
            log.info("IA Hermes desligada (hermes.ia.url vazio) — análise só por regras");
        }
    }

    public boolean habilitado() {
        return propriedades.habilitado();
    }

    public String modo() {
        return propriedades.modo();
    }

    public String url() {
        return propriedades.url();
    }

    /** O serviço Python respondeu ao {@code GET /saude}? */
    public boolean online() {
        if (!habilitado()) {
            return false;
        }
        try {
            JsonNode saude = http.get().uri(propriedades.url() + "/saude").retrieve().body(JsonNode.class);
            return saude != null && "ok".equals(saude.path("status").asText());
        } catch (RestClientException e) {
            return false;
        }
    }

    /**
     * Analisa uma transcrição. Lança {@link IaIndisponivelException} se a IA estiver
     * desligada ou não responder — usado pelo endpoint que expõe a IA diretamente.
     */
    public JsonNode analisar(String transcricao, String modo) {
        if (!habilitado()) {
            throw new IaIndisponivelException(
                    "A IA não está configurada. Suba o serviço Python e defina HERMES_IA_URL.");
        }
        String modoEfetivo = modo == null || modo.isBlank() ? propriedades.modo() : modo;
        try {
            JsonNode resposta = http.post()
                    .uri(propriedades.url() + "/analisar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("transcricao", transcricao, "modo", modoEfetivo))
                    .retrieve()
                    .body(JsonNode.class);
            if (resposta == null || resposta.path("risco_churn").isMissingNode()) {
                throw new IaIndisponivelException("A IA devolveu uma resposta vazia ou inválida.");
            }
            return resposta;
        } catch (RestClientResponseException e) {
            throw new IaIndisponivelException("A IA respondeu HTTP " + e.getStatusCode().value() + ".", e);
        } catch (ResourceAccessException e) {
            throw new IaIndisponivelException("A IA não respondeu em " + propriedades.url()
                    + " (serviço fora do ar ou tempo esgotado).", e);
        } catch (RestClientException e) {
            throw new IaIndisponivelException("Resposta inválida da IA.", e);
        }
    }

    /**
     * Analisa várias transcrições numa chamada só ({@code POST /analisar-lote}). O serviço
     * passa os trechos de todas pelo modelo juntos, o que rende mais por reunião.
     * Usado pela {@link FilaAnaliseIa}.
     *
     * @return um resultado por transcrição, na mesma ordem
     */
    public List<JsonNode> analisarLote(List<String> transcricoes) {
        if (!habilitado()) {
            throw new IaIndisponivelException("A IA não está configurada.");
        }
        try {
            JsonNode resposta = http.post()
                    .uri(propriedades.url() + "/analisar-lote")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("transcricoes", transcricoes, "modo", propriedades.modo()))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode lista = resposta == null ? null : resposta.path("resultados");
            if (lista == null || !lista.isArray() || lista.size() != transcricoes.size()) {
                throw new IaIndisponivelException("A IA devolveu um lote incompleto.");
            }
            List<JsonNode> resultados = new ArrayList<>(lista.size());
            lista.forEach(resultados::add);
            return resultados;
        } catch (RestClientResponseException e) {
            throw new IaIndisponivelException("A IA respondeu HTTP " + e.getStatusCode().value() + ".", e);
        } catch (ResourceAccessException e) {
            throw new IaIndisponivelException("A IA não respondeu em " + propriedades.url()
                    + " (serviço fora do ar ou tempo esgotado).", e);
        } catch (RestClientException e) {
            throw new IaIndisponivelException("Resposta inválida da IA.", e);
        }
    }

    /**
     * Versão tolerante, usada na análise das reuniões: se a IA estiver desligada ou
     * falhar, devolve vazio e o sistema segue só com as regras (nunca derruba a importação).
     */
    public Optional<JsonNode> tentarAnalisar(String transcricao) {
        if (!habilitado() || transcricao == null || transcricao.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(analisar(transcricao, null));
        } catch (IaIndisponivelException e) {
            log.warn("IA indisponível, mantendo a análise por regras: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
