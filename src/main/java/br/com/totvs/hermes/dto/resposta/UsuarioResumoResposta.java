package br.com.totvs.hermes.dto.resposta;

import br.com.totvs.hermes.model.PerfilUsuario;

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
