package br.com.totvs.hermes.dao;

import br.com.totvs.hermes.model.PerfilUsuario;
import br.com.totvs.hermes.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Operações de acesso a dados de {@link Usuario}.
 */
public interface UsuarioDao extends GenericDao<Usuario, Long> {

    /** Busca um usuário pelo e-mail (chave natural de login). */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Verifica se o e-mail já pertence a outro usuário.
     *
     * @param email     e-mail a validar
     * @param idIgnorado id do próprio usuário em edição ({@code null} na inclusão)
     */
    boolean existeOutroComEmail(String email, Long idIgnorado);

    /** Lista os usuários de um determinado perfil. */
    List<Usuario> listarPorPerfil(PerfilUsuario perfil);

    /**
     * Pesquisa paginada por nome/e-mail.
     *
     * @param termo         texto livre (opcional)
     * @param somenteAtivos quando {@code true}, restringe aos usuários ativos
     */
    List<Usuario> pesquisar(String termo, Boolean somenteAtivos, int pagina, int tamanho);

    /** Total de registros correspondentes aos mesmos critérios de {@link #pesquisar}. */
    long contarPesquisa(String termo, Boolean somenteAtivos);
}
