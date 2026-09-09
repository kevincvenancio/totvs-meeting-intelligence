package br.com.totvs.insight360.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Beans relacionados à proteção de credenciais.
 * <p>
 * Apenas o módulo de criptografia do Spring Security é usado (BCrypt): a API não
 * possui filtro de autenticação, o login é feito pelo endpoint
 * {@code POST /api/v1/autenticacao/login} e a identificação do usuário nas demais
 * operações viaja no próprio corpo/parâmetro da requisição.
 */
@Configuration
public class ConfiguracaoSeguranca {

    /** Força do BCrypt: 10 rodadas é o padrão recomendado para uso geral. */
    private static final int FORCA_BCRYPT = 10;

    @Bean
    public PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder(FORCA_BCRYPT);
    }
}
