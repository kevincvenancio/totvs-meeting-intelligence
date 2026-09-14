package br.com.totvs.hermes.config;

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
    public OpenAPI documentacaoHermes() {
        return new OpenAPI().info(new Info()
                .title("TOTVS Hermes — API REST")
                .version("3.0.0")
                .description("""
                        Plataforma de inteligência comercial sobre reuniões TOTVS.

                        Endpoints para importação de transcrições, análise automática,
                        carteira de clientes, planos de ação, comentários e relatórios.
                        """)
                .contact(new Contact().name("TOTVS Hermes"))
                .license(new License().name("Uso acadêmico")));
    }
}
