package br.com.totvs.insight360.controller;

import br.com.totvs.insight360.dto.mapper.ComentarioMapper;
import br.com.totvs.insight360.dto.requisicao.AtualizacaoComentarioRequisicao;
import br.com.totvs.insight360.dto.requisicao.ComentarioRequisicao;
import br.com.totvs.insight360.dto.resposta.ComentarioResposta;
import br.com.totvs.insight360.dto.resposta.PaginaResposta;
import br.com.totvs.insight360.model.ComentarioReuniao;
import br.com.totvs.insight360.service.ComentarioReuniaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * CRUD dos comentários das reuniões.
 * <p>
 * A criação e a listagem ficam sob o recurso pai (a reunião); a edição e a
 * exclusão usam o identificador do próprio comentário.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Comentários", description = "Anotações do time sobre as reuniões analisadas")
public class ComentarioController {

    private final ComentarioReuniaoService comentarioService;

    @GetMapping("/reunioes/{reuniaoId}/comentarios")
    @Operation(summary = "Lista os comentários de uma reunião, do mais recente para o mais antigo")
    public ResponseEntity<PaginaResposta<ComentarioResposta>> listar(
            @PathVariable Long reuniaoId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página mínima é 0.") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho mínimo da página é 1.")
            @Max(value = 100, message = "O tamanho máximo da página é 100.") int tamanho) {

        List<ComentarioResposta> conteudo = ComentarioMapper.paraLista(
                comentarioService.listarPorReuniao(reuniaoId, pagina, tamanho));
        long total = comentarioService.contarPorReuniao(reuniaoId);
        return ResponseEntity.ok(PaginaResposta.de(conteudo, pagina, tamanho, total));
    }

    @PostMapping("/reunioes/{reuniaoId}/comentarios")
    @Operation(summary = "Registra um comentário em uma reunião")
    public ResponseEntity<ComentarioResposta> criar(@PathVariable Long reuniaoId,
                                                    @RequestBody @Valid ComentarioRequisicao requisicao) {
        ComentarioReuniao comentario = comentarioService.criar(reuniaoId, requisicao);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/comentarios/{id}")
                .buildAndExpand(comentario.getId())
                .toUri();
        return ResponseEntity.created(localizacao).body(ComentarioMapper.paraResposta(comentario));
    }

    @PutMapping("/comentarios/{id}")
    @Operation(summary = "Altera o texto de um comentário (apenas autor, gestor ou administrador)")
    public ResponseEntity<ComentarioResposta> atualizar(
            @PathVariable Long id, @RequestBody @Valid AtualizacaoComentarioRequisicao requisicao) {
        return ResponseEntity.ok(
                ComentarioMapper.paraResposta(comentarioService.atualizar(id, requisicao)));
    }

    @DeleteMapping("/comentarios/{id}")
    @Operation(summary = "Remove um comentário (apenas autor, gestor ou administrador)")
    public ResponseEntity<Void> remover(
            @PathVariable Long id,
            @RequestParam @NotNull(message = "Informe o usuário que está removendo o comentário.")
            Long usuarioId) {
        comentarioService.remover(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
