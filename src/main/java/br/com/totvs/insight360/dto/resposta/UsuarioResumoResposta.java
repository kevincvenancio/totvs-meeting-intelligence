package br.com.totvs.insight360.dto.resposta;

import br.com.totvs.insight360.model.PerfilUsuario;

/**
 * Identificação resumida de um usuário, usada dentro de outras respostas
 * (autor de comentário, responsável por plano de ação).
 */
public record UsuarioResumoResposta(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil
) {
}
