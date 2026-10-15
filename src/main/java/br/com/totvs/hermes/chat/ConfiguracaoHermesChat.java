package br.com.totvs.hermes.chat;

import br.com.totvs.hermes.chat.llm.HermesLlmPropriedades;
import br.com.totvs.hermes.chat.llm.LlmClient;
import br.com.totvs.hermes.chat.llm.LlmComFallback;
import br.com.totvs.hermes.chat.llm.ProvedorOpenAiCompat;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Monta a cadeia de provedores de LLM e o rate limit a partir do application.properties.
 */
@Configuration
@EnableConfigurationProperties({HermesChatPropriedades.class, HermesLlmPropriedades.class})
public class ConfiguracaoHermesChat {

    @Bean
    public LlmClient hermesLlm(HermesLlmPropriedades llm, HermesChatPropriedades chat) {
        Duration timeout = Duration.ofSeconds(chat.timeoutSegundos());
        List<ProvedorOpenAiCompat> provedores = llm.provedores().stream()
                .map(p -> new ProvedorOpenAiCompat(p, timeout))
                .toList();
        return new LlmComFallback(provedores);
    }

    @Bean
    public LimiteRequisicoes hermesLimiteRequisicoes(HermesChatPropriedades chat) {
        return new LimiteRequisicoes(chat.limitePorMinutoIp());
    }
}
