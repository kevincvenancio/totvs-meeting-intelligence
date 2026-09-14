package br.com.totvs.hermes.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração da camada web: libera o consumo da API pelo Front-End.
 * <p>
 * As origens permitidas ficam em {@code hermes.cors.origens}, no
 * application.properties, para não exigir recompilação a cada ambiente.
 */
@Configuration
@RequiredArgsConstructor
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private static final String PADRAO_API = "/api/**";

    @Value("${hermes.cors.origens:*}")
    private String[] origensPermitidas;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registro) {
        registro.addMapping(PADRAO_API)
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location", "Content-Disposition")
                .maxAge(3600);
    }
}
