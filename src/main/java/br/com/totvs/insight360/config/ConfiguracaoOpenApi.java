package br.com.totvs.insight360.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação da API (OpenAPI 3 / Swagger UI).
 * Disponível em {@code /swagger-ui.html} com a aplicação em execução.
 */
@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    public OpenAPI documentacaoInsight360() {
        return new OpenAPI().info(new Info()
                .title("TOTVS Insight360 — API REST")
                .version("3.0.0")
                .description("""
                        Plataforma de inteligência comercial sobre reuniões TOTVS.

                        Endpoints para importação de transcrições, análise automática,
                        carteira de clientes, planos de ação, comentários e relatórios.
                        """)
                .contact(new Contact().name("TOTVS Insight360"))
                .license(new License().name("Uso acadêmico")));
    }
}
