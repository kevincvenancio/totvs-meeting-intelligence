package br.com.totvs.insight360.dto.resposta;

import br.com.totvs.insight360.model.PerfilUsuario;

import java.time.LocalDateTime;

/**
 * Representação de um usuário na API. O hash da senha nunca é exposto.
 */
public record UsuarioResposta(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        String perfilDescricao,
        String cargo,
        boolean ativo,
        LocalDateTime dataCadastro,
        LocalDateTime dataAtualizacao
) {
}
