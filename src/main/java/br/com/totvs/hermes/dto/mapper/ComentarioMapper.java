package br.com.totvs.hermes.dto.mapper;

import br.com.totvs.hermes.dto.resposta.ComentarioResposta;
import br.com.totvs.hermes.model.ComentarioReuniao;

import java.util.List;

/**
 * Conversões entre {@link ComentarioReuniao} e seus DTOs (padrão Mapper/Assembler).
 */
public final class ComentarioMapper {

    private ComentarioMapper() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    public static ComentarioResposta paraResposta(ComentarioReuniao comentario) {
        return new ComentarioResposta(
                comentario.getId(),
                comentario.getReuniao() != null ? comentario.getReuniao().getId() : null,
                comentario.getTexto(),
                comentario.isEditado(),
                UsuarioMapper.paraResumo(comentario.getAutor()),
                comentario.getDataCriacao(),
                comentario.getDataAtualizacao());
    }

    public static List<ComentarioResposta> paraLista(List<ComentarioReuniao> comentarios) {
        return comentarios.stream().map(ComentarioMapper::paraResposta).toList();
    }
}
