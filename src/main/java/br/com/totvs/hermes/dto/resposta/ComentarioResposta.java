package br.com.totvs.hermes.dto.resposta;

import java.time.LocalDateTime;

/**
 * Comentário de um usuário sobre uma reunião.
 */
public record ComentarioResposta(
        Long id,
        Long reuniaoId,
        String texto,
        boolean editado,
        UsuarioResumoResposta autor,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
}
