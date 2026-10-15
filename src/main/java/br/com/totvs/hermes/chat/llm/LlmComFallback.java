package br.com.totvs.hermes.chat.llm;

import br.com.totvs.hermes.exception.HermesIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Tenta os provedores na ordem configurada. Provedor sem chave é pulado; em
 * qualquer falha (402, 429, 5xx, timeout, resposta vazia) cai para o próximo.
 * Se nenhum responder, lança {@link HermesIndisponivelException} (HTTP 503).
 */
public class LlmComFallback implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmComFallback.class);

    private final List<ProvedorLlm> provedores;

    public LlmComFallback(List<? extends ProvedorLlm> provedores) {
        this.provedores = provedores.stream().filter(ProvedorLlm::configurado).map(p -> (ProvedorLlm) p).toList();
        if (this.provedores.isEmpty()) {
            log.warn("Hermes Chat: nenhum provedor de LLM com chave configurada — o chat responderá 503.");
        } else {
            log.info("Hermes Chat: provedores ativos = {}",
                    this.provedores.stream().map(ProvedorLlm::nome).toList());
        }
    }

    /** Quantos provedores estão realmente na cadeia (com chave). */
    public int quantidadeAtivos() {
        return provedores.size();
    }

    @Override
    public String completar(List<Mensagem> mensagens) {
        for (ProvedorLlm provedor : provedores) {
            try {
                return provedor.completar(mensagens);
            } catch (ProvedorLlmException e) {
                log.warn("Hermes Chat: provedor {} falhou ({}); tentando o próximo.", provedor.nome(), e.getMessage());
            } catch (RuntimeException e) {
                log.warn("Hermes Chat: erro inesperado no provedor {}: {}", provedor.nome(), e.toString());
            }
        }
        throw new HermesIndisponivelException();
    }
}
