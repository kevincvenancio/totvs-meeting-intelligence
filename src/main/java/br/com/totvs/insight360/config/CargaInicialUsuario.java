package br.com.totvs.insight360.config;

import br.com.totvs.insight360.dao.UsuarioDao;
import br.com.totvs.insight360.model.PerfilUsuario;
import br.com.totvs.insight360.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o administrador padrão quando o banco ainda não possui nenhum usuário.
 * <p>
 * Sem esse registro inicial não seria possível autenticar no sistema nem cadastrar
 * os demais usuários — o cadastro é feito pela própria API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CargaInicialUsuario implements ApplicationRunner {

    private static final String EMAIL_PADRAO = "admin@insight360.com.br";
    private static final String SENHA_PADRAO = "admin123";

    private final UsuarioDao usuarioDao;
    private final PasswordEncoder codificadorSenha;

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (usuarioDao.contar() > 0) {
            return;
        }
        Usuario administrador = new Usuario();
        administrador.setNome("Administrador Insight360");
        administrador.setEmail(EMAIL_PADRAO);
        administrador.setSenhaHash(codificadorSenha.encode(SENHA_PADRAO));
        administrador.setPerfil(PerfilUsuario.ADMIN);
        administrador.setCargo("Administrador");
        administrador.setAtivo(Boolean.TRUE);
        usuarioDao.inserir(administrador);

        log.warn("Usuario administrador padrao criado: {} / {} — altere a senha no primeiro acesso.",
                EMAIL_PADRAO, SENHA_PADRAO);
    }
}
