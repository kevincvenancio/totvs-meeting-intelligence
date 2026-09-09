package br.com.totvs.insight360.dto.mapper;

import br.com.totvs.insight360.dto.requisicao.AtualizacaoUsuarioRequisicao;
import br.com.totvs.insight360.dto.requisicao.UsuarioRequisicao;
import br.com.totvs.insight360.dto.resposta.UsuarioResposta;
import br.com.totvs.insight360.dto.resposta.UsuarioResumoResposta;
import br.com.totvs.insight360.model.Usuario;

import java.util.List;

/**
 * Conversões entre {@link Usuario} e seus DTOs (padrão Mapper/Assembler).
 * <p>
 * Manter a conversão fora da entidade e fora do controller evita expor o modelo
 * de persistência na API — em especial o hash da senha, que nunca é mapeado.
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    /** Converte a requisição de cadastro em entidade, já com a senha criptografada. */
    public static Usuario paraEntidade(UsuarioRequisicao requisicao, String senhaHash) {
        Usuario usuario = new Usuario();
        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(requisicao.email().trim().toLowerCase());
        usuario.setSenhaHash(senhaHash);
        usuario.setPerfil(requisicao.perfil());
        usuario.setCargo(requisicao.cargo());
        usuario.setAtivo(Boolean.TRUE);
        return usuario;
    }

    /** Aplica os dados editáveis sobre uma entidade existente. */
    public static void aplicar(AtualizacaoUsuarioRequisicao requisicao, Usuario usuario) {
        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(requisicao.email().trim().toLowerCase());
        usuario.setPerfil(requisicao.perfil());
        usuario.setCargo(requisicao.cargo());
        usuario.setAtivo(requisicao.ativo());
    }

    public static UsuarioResposta paraResposta(Usuario usuario) {
        return new UsuarioResposta(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getPerfil() != null ? usuario.getPerfil().getDescricao() : null,
                usuario.getCargo(),
                usuario.isAtivo(),
                usuario.getDataCadastro(),
                usuario.getDataAtualizacao());
    }

    public static UsuarioResumoResposta paraResumo(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumoResposta(usuario.getId(), usuario.getNome(),
                usuario.getEmail(), usuario.getPerfil());
    }

    public static List<UsuarioResposta> paraLista(List<Usuario> usuarios) {
        return usuarios.stream().map(UsuarioMapper::paraResposta).toList();
    }
}
