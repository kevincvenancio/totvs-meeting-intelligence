package br.com.totvs.insight360.controller;

import br.com.totvs.insight360.dto.mapper.UsuarioMapper;
import br.com.totvs.insight360.dto.requisicao.AlteracaoSenhaRequisicao;
import br.com.totvs.insight360.dto.requisicao.AtualizacaoUsuarioRequisicao;
import br.com.totvs.insight360.dto.requisicao.UsuarioRequisicao;
import br.com.totvs.insight360.dto.resposta.PaginaResposta;
import br.com.totvs.insight360.dto.resposta.UsuarioResposta;
import br.com.totvs.insight360.model.PerfilUsuario;
import br.com.totvs.insight360.model.Usuario;
import br.com.totvs.insight360.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * CRUD de usuários da plataforma.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Validated
@Tag(name = "Usuários", description = "Cadastro e manutenção dos usuários da plataforma")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Lista usuários de forma paginada, com pesquisa por nome ou e-mail")
    public ResponseEntity<PaginaResposta<UsuarioResposta>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Boolean ativos,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página mínima é 0.") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho mínimo da página é 1.")
            @Max(value = 100, message = "O tamanho máximo da página é 100.") int tamanho) {

        List<UsuarioResposta> conteudo =
                UsuarioMapper.paraLista(usuarioService.pesquisar(termo, ativos, pagina, tamanho));
        long total = usuarioService.contarPesquisa(termo, ativos);
        return ResponseEntity.ok(PaginaResposta.de(conteudo, pagina, tamanho, total));
    }

    @GetMapping("/perfis/{perfil}")
    @Operation(summary = "Lista os usuários de um perfil, para preencher seleções no Front-End")
    public ResponseEntity<List<UsuarioResposta>> listarPorPerfil(@PathVariable PerfilUsuario perfil) {
        return ResponseEntity.ok(UsuarioMapper.paraLista(usuarioService.listarPorPerfil(perfil)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário pelo identificador")
    public ResponseEntity<UsuarioResposta> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(UsuarioMapper.paraResposta(usuarioService.buscarPorId(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo usuário")
    public ResponseEntity<UsuarioResposta> cadastrar(@RequestBody @Valid UsuarioRequisicao requisicao) {
        Usuario usuario = usuarioService.cadastrar(requisicao);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(usuario.getId())
                .toUri();
        return ResponseEntity.created(localizacao).body(UsuarioMapper.paraResposta(usuario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados cadastrais de um usuário")
    public ResponseEntity<UsuarioResposta> atualizar(@PathVariable Long id,
                                                     @RequestBody @Valid AtualizacaoUsuarioRequisicao requisicao) {
        return ResponseEntity.ok(UsuarioMapper.paraResposta(usuarioService.atualizar(id, requisicao)));
    }

    @PatchMapping("/{id}/senha")
    @Operation(summary = "Altera a senha do usuário mediante confirmação da senha atual")
    public ResponseEntity<Void> alterarSenha(@PathVariable Long id,
                                             @RequestBody @Valid AlteracaoSenhaRequisicao requisicao) {
        usuarioService.alterarSenha(id, requisicao);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um usuário sem histórico no sistema")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
