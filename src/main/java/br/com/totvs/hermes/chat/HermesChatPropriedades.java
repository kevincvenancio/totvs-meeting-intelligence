package br.com.totvs.hermes.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parâmetros do chat Hermes ({@code hermes.chat.*} no application.properties).
 *
 * @param habilitado               liga/desliga o endpoint sem precisar de novo deploy
 * @param maxCaracteresPergunta    tamanho máximo de uma pergunta
 * @param maxTurnosHistorico       mensagens anteriores enviadas ao modelo
 * @param limitePorMinutoIp        perguntas por minuto aceitas de um mesmo IP
 * @param timeoutSegundos          tempo máximo de espera por provedor
 */
@ConfigurationProperties(prefix = "hermes.chat")
public record HermesChatPropriedades(
        @DefaultValue("true") boolean habilitado,
        @DefaultValue("600") int maxCaracteresPergunta,
        @DefaultValue("6") int maxTurnosHistorico,
        @DefaultValue("8") int limitePorMinutoIp,
        @DefaultValue("40") int timeoutSegundos
) {
}
