package br.com.totvs.hermes.dto.resposta;

import br.com.totvs.hermes.model.PerfilUsuario;

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
