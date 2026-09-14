package br.com.totvs.hermes.dao;

import br.com.totvs.hermes.model.ComentarioReuniao;

import java.util.List;
import java.util.Optional;

/**
 * Operações de acesso a dados de {@link ComentarioReuniao}.
 */
public interface ComentarioReuniaoDao extends GenericDao<ComentarioReuniao, Long> {

    /** Comentários de uma reunião, do mais recente para o mais antigo. */
    List<ComentarioReuniao> listarPorReuniao(Long reuniaoId);

    /** Comentários de uma reunião de forma paginada. */
    List<ComentarioReuniao> listarPorReuniao(Long reuniaoId, int pagina, int tamanho);

    /** Busca o comentário já com reunião e autor carregados. */
    Optional<ComentarioReuniao> buscarDetalhado(Long id);

    /** Quantidade de comentários de uma reunião. */
    long contarPorReuniao(Long reuniaoId);

    /** Quantidade de comentários escritos por um autor. */
    long contarPorAutor(Long autorId);

    /** Remove todos os comentários de uma reunião (usado na exclusão em cascata). */
    int removerPorReuniao(Long reuniaoId);
}
