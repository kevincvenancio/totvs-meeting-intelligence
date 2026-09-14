package br.com.totvs.hermes.service;

import br.com.totvs.hermes.dao.ComentarioReuniaoDao;
import br.com.totvs.hermes.dao.PlanoAcaoDao;
import br.com.totvs.hermes.dao.UsuarioDao;
import br.com.totvs.hermes.dto.mapper.UsuarioMapper;
import br.com.totvs.hermes.dto.requisicao.AlteracaoSenhaRequisicao;
import br.com.totvs.hermes.dto.requisicao.AtualizacaoUsuarioRequisicao;
import br.com.totvs.hermes.dto.requisicao.LoginRequisicao;
import br.com.totvs.hermes.dto.requisicao.UsuarioRequisicao;
import br.com.totvs.hermes.exception.ConflitoDeDadosException;
import br.com.totvs.hermes.exception.CredenciaisInvalidasException;
import br.com.totvs.hermes.exception.RecursoNaoEncontradoException;
import br.com.totvs.hermes.exception.RegraNegocioException;
import br.com.totvs.hermes.model.PerfilUsuario;
import br.com.totvs.hermes.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negócio de {@link Usuario}: cadastro, edição, inativação,
 * troca de senha e autenticação.
 * <p>
 * Toda a validação acontece aqui — os controllers apenas recebem a requisição já
 * validada pelo Bean Validation e delegam.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UsuarioService {

    private static final String RECURSO = "Usuário";

    private final UsuarioDao usuarioDao;
    private final PlanoAcaoDao planoAcaoDao;
    private final ComentarioReuniaoDao comentarioDao;
    private final PasswordEncoder codificadorSenha;

    // ── CRUD ──────────────────────────────────────────────────────────

    /**
     * Cadastra um novo usuário com a senha criptografada.
     *
     * @throws ConflitoDeDadosException quando o e-mail já está em uso
     */
    @Transactional
    public Usuario cadastrar(UsuarioRequisicao requisicao) {
        validarEmailDisponivel(requisicao.email(), null);
        Usuario usuario = UsuarioMapper.paraEntidade(requisicao,
                codificadorSenha.encode(requisicao.senha()));
        Usuario salvo = usuarioDao.inserir(usuario);
        log.info("Usuario cadastrado: #{} - {}", salvo.getId(), salvo.getEmail());
        return salvo;
    }

    /**
     * Atualiza os dados cadastrais de um usuário.
     *
     * @throws RecursoNaoEncontradoException quando o id não existe
     * @throws ConflitoDeDadosException      quando o e-mail pertence a outro usuário
     * @throws RegraNegocioException         ao inativar o último administrador ativo
     */
    @Transactional
    public Usuario atualizar(Long id, AtualizacaoUsuarioRequisicao requisicao) {
        Usuario usuario = buscarPorId(id);
        validarEmailDisponivel(requisicao.email(), id);
        validarPermanenciaDeAdministrador(usuario, requisicao.perfil(), requisicao.ativo());
        UsuarioMapper.aplicar(requisicao, usuario);
        return usuarioDao.atualizar(usuario);
    }

    /**
     * Remove um usuário. Usuários com histórico (planos de ação ou comentários)
     * não podem ser excluídos — devem ser inativados para preservar o histórico.
     */
    @Transactional
    public void remover(Long id) {
        Usuario usuario = buscarPorId(id);
        if (planoAcaoDao.existeEmAbertoParaResponsavel(id)) {
            throw new RegraNegocioException(
                    "O usuário possui planos de ação em aberto. Transfira a responsabilidade "
                            + "ou finalize os planos antes de excluí-lo.");
        }
        if (comentarioDao.contarPorAutor(id) > 0 || !planoAcaoDao.listarPorResponsavel(id).isEmpty()) {
            throw new RegraNegocioException(
                    "O usuário possui histórico registrado no sistema. Inative-o em vez de excluí-lo.");
        }
        validarPermanenciaDeAdministrador(usuario, null, Boolean.FALSE);
        usuarioDao.remover(usuario);
        log.info("Usuario removido: #{}", id);
    }

    /** Busca um usuário pelo id. */
    public Usuario buscarPorId(Long id) {
        return usuarioDao.buscarPorId(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de(RECURSO, id));
    }

    /** Pesquisa paginada por nome ou e-mail. */
    public List<Usuario> pesquisar(String termo, Boolean somenteAtivos, int pagina, int tamanho) {
        return usuarioDao.pesquisar(termo, somenteAtivos, pagina, tamanho);
    }

    /** Total de registros correspondentes aos mesmos critérios de {@link #pesquisar}. */
    public long contarPesquisa(String termo, Boolean somenteAtivos) {
        return usuarioDao.contarPesquisa(termo, somenteAtivos);
    }

    /** Lista os usuários de um perfil — usado para montar combos no Front-End. */
    public List<Usuario> listarPorPerfil(PerfilUsuario perfil) {
        return usuarioDao.listarPorPerfil(perfil);
    }

    // ── Senha e autenticação ──────────────────────────────────────────

    /**
     * Troca a senha do usuário mediante confirmação da senha atual.
     *
     * @throws RegraNegocioException quando a senha atual não confere ou a nova é igual à anterior
     */
    @Transactional
    public void alterarSenha(Long id, AlteracaoSenhaRequisicao requisicao) {
        Usuario usuario = buscarPorId(id);
        if (!codificadorSenha.matches(requisicao.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual informada está incorreta.");
        }
        if (codificadorSenha.matches(requisicao.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da senha atual.");
        }
        usuario.setSenhaHash(codificadorSenha.encode(requisicao.novaSenha()));
        usuarioDao.atualizar(usuario);
        log.info("Senha alterada para o usuario #{}", id);
    }

    /**
     * Autentica um usuário pelo e-mail e senha.
     *
     * @throws CredenciaisInvalidasException quando o e-mail não existe, a senha
     *                                       não confere ou o usuário está inativo
     */
    public Usuario autenticar(LoginRequisicao requisicao) {
        Usuario usuario = usuarioDao.buscarPorEmail(requisicao.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos."));
        if (!codificadorSenha.matches(requisicao.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos.");
        }
        if (!usuario.isAtivo()) {
            throw new CredenciaisInvalidasException("Usuário inativo. Procure o administrador do sistema.");
        }
        return usuario;
    }

    /**
     * Recupera um usuário ativo — usado quando outra operação precisa identificar
     * quem está agindo (autor de comentário, responsável por plano de ação).
     *
     * @throws RegraNegocioException quando o usuário está inativo
     */
    public Usuario buscarAtivoPorId(Long id) {
        Usuario usuario = buscarPorId(id);
        if (!usuario.isAtivo()) {
            throw new RegraNegocioException(
                    "O usuário " + usuario.getNome() + " está inativo e não pode executar esta operação.");
        }
        return usuario;
    }

    // ── Validações internas ───────────────────────────────────────────

    private void validarEmailDisponivel(String email, Long idIgnorado) {
        if (usuarioDao.existeOutroComEmail(email, idIgnorado)) {
            throw new ConflitoDeDadosException("Já existe um usuário cadastrado com o e-mail " + email + ".");
        }
    }

    /**
     * Impede que o sistema fique sem nenhum administrador ativo.
     *
     * @param novoPerfil perfil que será gravado ({@code null} em exclusão)
     * @param novoAtivo  situação que será gravada
     */
    private void validarPermanenciaDeAdministrador(Usuario usuario, PerfilUsuario novoPerfil, Boolean novoAtivo) {
        boolean eraAdministradorAtivo = PerfilUsuario.ADMIN == usuario.getPerfil() && usuario.isAtivo();
        boolean continuaAdministradorAtivo = PerfilUsuario.ADMIN == novoPerfil && Boolean.TRUE.equals(novoAtivo);
        if (!eraAdministradorAtivo || continuaAdministradorAtivo) {
            return;
        }
        long administradoresAtivos = usuarioDao.listarPorPerfil(PerfilUsuario.ADMIN).stream()
                .filter(Usuario::isAtivo)
                .count();
        if (administradoresAtivos <= 1) {
            throw new RegraNegocioException(
                    "Este é o último administrador ativo do sistema; a operação deixaria a plataforma sem administrador.");
        }
    }
}
