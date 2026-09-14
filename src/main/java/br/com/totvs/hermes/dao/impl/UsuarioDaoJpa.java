package br.com.totvs.hermes.dao.impl;

import br.com.totvs.hermes.dao.AbstractJpaDao;
import br.com.totvs.hermes.dao.UsuarioDao;
import br.com.totvs.hermes.model.PerfilUsuario;
import br.com.totvs.hermes.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação JPA de {@link UsuarioDao}.
 */
@Repository
public class UsuarioDaoJpa extends AbstractJpaDao<Usuario, Long> implements UsuarioDao {

    private static final String SELECT_BASE = "SELECT u FROM Usuario u";
    private static final String COUNT_BASE = "SELECT COUNT(u) FROM Usuario u";

    public UsuarioDaoJpa() {
        super(Usuario.class);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return primeiroResultado(
                criarConsulta(SELECT_BASE + " WHERE LOWER(u.email) = :email")
                        .setParameter("email", email.trim().toLowerCase()));
    }

    @Override
    public boolean existeOutroComEmail(String email, Long idIgnorado) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("email", email.trim().toLowerCase());
        String jpql = COUNT_BASE + " WHERE LOWER(u.email) = :email";
        if (idIgnorado != null) {
            jpql += " AND u.id <> :idIgnorado";
            parametros.put("idIgnorado", idIgnorado);
        }
        return contarPor(jpql, parametros) > 0;
    }

    @Override
    public List<Usuario> listarPorPerfil(PerfilUsuario perfil) {
        return criarConsulta(SELECT_BASE + " WHERE u.perfil = :perfil ORDER BY u.nome")
                .setParameter("perfil", perfil)
                .getResultList();
    }

    @Override
    public List<Usuario> pesquisar(String termo, Boolean somenteAtivos, int pagina, int tamanho) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(termo, somenteAtivos, parametros);
        return paginar(criarConsulta(SELECT_BASE + clausulas + " ORDER BY u.nome", parametros),
                pagina, tamanho).getResultList();
    }

    @Override
    public long contarPesquisa(String termo, Boolean somenteAtivos) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(termo, somenteAtivos, parametros);
        return contarPor(COUNT_BASE + clausulas, parametros);
    }

    /** Monta o WHERE dinâmico compartilhado entre a pesquisa e a contagem. */
    private String montarClausulas(String termo, Boolean somenteAtivos, Map<String, Object> parametros) {
        StringBuilder clausulas = new StringBuilder(" WHERE 1 = 1");
        if (termo != null && !termo.isBlank()) {
            clausulas.append(" AND (LOWER(u.nome) LIKE :termo OR LOWER(u.email) LIKE :termo)");
            parametros.put("termo", "%" + termo.trim().toLowerCase() + "%");
        }
        if (somenteAtivos != null) {
            clausulas.append(" AND u.ativo = :ativo");
            parametros.put("ativo", somenteAtivos);
        }
        return clausulas.toString();
    }
}
