package br.com.totvs.hermes.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do serviço de IA (pasta {@code ciencia-de-dados}, modelos do Hugging Face).
 *
 * @param url             endereço do serviço Python (ex.: {@code http://localhost:8000}).
 *                        Vazio = IA desligada e a análise usa só as regras.
 * @param modo            {@code hibrido} (padrão), {@code modelo} ou {@code heuristico}
 * @param timeoutSegundos tempo máximo de uma chamada (um lote de reuniões longas em CPU leva ~15 s)
 * @param lote            reuniões enviadas por chamada pela fila em segundo plano
 */
@ConfigurationProperties(prefix = "hermes.ia")
public record HermesIaPropriedades(String url, String modo, Integer timeoutSegundos, Integer lote) {

    public HermesIaPropriedades {
        url = url == null ? "" : url.trim().replaceAll("/+$", "");
        modo = modo == null || modo.isBlank() ? "hibrido" : modo.trim();
        timeoutSegundos = timeoutSegundos == null || timeoutSegundos <= 0 ? 120 : timeoutSegundos;
        lote = lote == null || lote <= 0 ? 8 : Math.min(lote, 32);
    }

    public boolean habilitado() {
        return !url.isBlank();
    }
}
