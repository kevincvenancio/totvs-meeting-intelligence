package br.com.totvs.hermes.service;

import br.com.totvs.hermes.dao.ComentarioReuniaoDao;
import br.com.totvs.hermes.dao.ReuniaoDao;
import br.com.totvs.hermes.dto.requisicao.AtualizacaoComentarioRequisicao;
import br.com.totvs.hermes.dto.requisicao.ComentarioRequisicao;
import br.com.totvs.hermes.exception.OperacaoNaoPermitidaException;
import br.com.totvs.hermes.exception.RecursoNaoEncontradoException;
import br.com.totvs.hermes.model.ComentarioReuniao;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negócio dos comentários registrados sobre uma reunião.
 * <p>
 * Edição e exclusão só são permitidas ao próprio autor ou a usuários com perfil
 * de gestão, regra centralizada em {@link Usuario#podeAlterarRegistroDe(Usuario)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ComentarioReuniaoService {

    private static final String RECURSO = "Comentário";

    private final ComentarioReuniaoDao comentarioDao;
    private final ReuniaoDao reuniaoDao;
    private final UsuarioService usuarioService;

    // ── CRUD ──────────────────────────────────────────────────────────

    /**
     * Registra um comentário em uma reunião.
     *
     * @throws RecursoNaoEncontradoException quando a reunião ou o autor não existem
     */
    @Transactional
    public ComentarioReuniao criar(Long reuniaoId, ComentarioRequisicao requisicao) {
        Reuniao reuniao = buscarReuniao(reuniaoId);
        Usuario autor = usuarioService.buscarAtivoPorId(requisicao.autorId());

        ComentarioReuniao comentario = new ComentarioReuniao();
        comentario.setReuniao(reuniao);
        comentario.setAutor(autor);
        comentario.setTexto(requisicao.texto().trim());

        ComentarioReuniao salvo = comentarioDao.inserir(comentario);
        log.info("Comentario #{} criado na reuniao #{} por #{}", salvo.getId(), reuniaoId, autor.getId());
        return salvo;
    }

    /**
     * Altera o texto de um comentário.
     *
     * @throws OperacaoNaoPermitidaException quando o usuário não é o autor nem tem perfil de gestão
     */
    @Transactional
    public ComentarioReuniao atualizar(Long id, AtualizacaoComentarioRequisicao requisicao) {
        ComentarioReuniao comentario = buscarPorId(id);
        garantirPermissao(comentario, requisicao.usuarioId());
        comentario.alterarTexto(requisicao.texto().trim());
        return comentarioDao.atualizar(comentario);
    }

    /**
     * Remove um comentário.
     *
     * @param usuarioId usuário que está solicitando a exclusão
     */
    @Transactional
    public void remover(Long id, Long usuarioId) {
        ComentarioReuniao comentario = buscarPorId(id);
        garantirPermissao(comentario, usuarioId);
        comentarioDao.remover(comentario);
        log.info("Comentario #{} removido pelo usuario #{}", id, usuarioId);
    }

    /** Busca o comentário com reunião e autor carregados. */
    public ComentarioReuniao buscarPorId(Long id) {
        return comentarioDao.buscarDetalhado(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de(RECURSO, id));
    }

    // ── Consultas ─────────────────────────────────────────────────────

    /** Comentários de uma reunião, dos mais recentes para os mais antigos. */
    public List<ComentarioReuniao> listarPorReuniao(Long reuniaoId, int pagina, int tamanho) {
        buscarReuniao(reuniaoId);
        return comentarioDao.listarPorReuniao(reuniaoId, pagina, tamanho);
    }

    /** Quantidade de comentários de uma reunião. */
    public long contarPorReuniao(Long reuniaoId) {
        return comentarioDao.contarPorReuniao(reuniaoId);
    }

    // ── Validações internas ───────────────────────────────────────────

    private Reuniao buscarReuniao(Long reuniaoId) {
        return reuniaoDao.buscarPorId(reuniaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Reunião", reuniaoId));
    }

    private void garantirPermissao(ComentarioReuniao comentario, Long usuarioId) {
        Usuario solicitante = usuarioService.buscarAtivoPorId(usuarioId);
        if (!solicitante.podeAlterarRegistroDe(comentario.getAutor())) {
            throw new OperacaoNaoPermitidaException(
                    "Apenas o autor do comentário, um gestor ou um administrador podem alterá-lo.");
        }
    }
}
