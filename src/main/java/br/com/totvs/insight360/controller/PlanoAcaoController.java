package br.com.totvs.insight360.controller;

import br.com.totvs.insight360.dto.filtro.FiltroPlanoAcao;
import br.com.totvs.insight360.dto.mapper.PlanoAcaoMapper;
import br.com.totvs.insight360.dto.requisicao.AtualizacaoPlanoAcaoRequisicao;
import br.com.totvs.insight360.dto.requisicao.PlanoAcaoRequisicao;
import br.com.totvs.insight360.dto.requisicao.TransicaoStatusRequisicao;
import br.com.totvs.insight360.dto.resposta.IndicadoresPlanoAcaoResposta;
import br.com.totvs.insight360.dto.resposta.PaginaResposta;
import br.com.totvs.insight360.dto.resposta.PlanoAcaoResposta;
import br.com.totvs.insight360.model.PlanoAcao;
import br.com.totvs.insight360.model.PrioridadePlanoAcao;
import br.com.totvs.insight360.model.StatusPlanoAcao;
import br.com.totvs.insight360.service.PlanoAcaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * CRUD dos planos de ação gerados a partir das reuniões.
 */
@RestController
@RequestMapping("/api/v1/planos-acao")
@RequiredArgsConstructor
@Validated
@Tag(name = "Planos de ação", description = "Acompanhamento das ações comerciais das reuniões")
public class PlanoAcaoController {

    private final PlanoAcaoService planoAcaoService;

    @GetMapping
    @Operation(summary = "Lista planos de ação de forma paginada, aplicando os filtros informados")
    public ResponseEntity<PaginaResposta<PlanoAcaoResposta>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Long reuniaoId,
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) StatusPlanoAcao status,
            @RequestParam(required = false) PrioridadePlanoAcao prioridade,
            @RequestParam(required = false) Boolean somenteAtrasados,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazoAte,
            @RequestParam(required = false) String ordenarPor,
            @RequestParam(required = false) String direcao,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página mínima é 0.") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho mínimo da página é 1.")
            @Max(value = 100, message = "O tamanho máximo da página é 100.") int tamanho) {

        FiltroPlanoAcao filtro = new FiltroPlanoAcao(termo, reuniaoId, responsavelId, status, prioridade,
                somenteAtrasados, prazoAte, ordenarPor, direcao);

        List<PlanoAcaoResposta> conteudo =
                PlanoAcaoMapper.paraLista(planoAcaoService.pesquisar(filtro, pagina, tamanho));
        return ResponseEntity.ok(
                PaginaResposta.de(conteudo, pagina, tamanho, planoAcaoService.contar(filtro)));
    }

    @GetMapping("/atrasados")
    @Operation(summary = "Lista os planos em aberto com prazo vencido")
    public ResponseEntity<List<PlanoAcaoResposta>> listarAtrasados() {
        return ResponseEntity.ok(PlanoAcaoMapper.paraLista(planoAcaoService.listarAtrasados()));
    }

    @GetMapping("/responsaveis/{responsavelId}")
    @Operation(summary = "Lista os planos atribuídos a um responsável")
    public ResponseEntity<List<PlanoAcaoResposta>> listarPorResponsavel(@PathVariable Long responsavelId) {
        return ResponseEntity.ok(
                PlanoAcaoMapper.paraLista(planoAcaoService.listarPorResponsavel(responsavelId)));
    }

    @GetMapping("/indicadores")
    @Operation(summary = "Resumo quantitativo dos planos por status e em atraso")
    public ResponseEntity<IndicadoresPlanoAcaoResposta> indicadores() {
        Map<String, Long> porStatus = planoAcaoService.contarPorStatus();
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long emAberto = porStatus.getOrDefault(StatusPlanoAcao.PENDENTE.name(), 0L)
                + porStatus.getOrDefault(StatusPlanoAcao.EM_ANDAMENTO.name(), 0L);
        return ResponseEntity.ok(new IndicadoresPlanoAcaoResposta(porStatus, total, emAberto,
                planoAcaoService.contarAtrasados()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um plano de ação pelo identificador")
    public ResponseEntity<PlanoAcaoResposta> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(PlanoAcaoMapper.paraResposta(planoAcaoService.buscarPorId(id)));
    }

    @PostMapping
    @Operation(summary = "Cria um plano de ação para uma reunião")
    public ResponseEntity<PlanoAcaoResposta> criar(@RequestBody @Valid PlanoAcaoRequisicao requisicao) {
        PlanoAcao plano = planoAcaoService.criar(requisicao);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(plano.getId())
                .toUri();
        return ResponseEntity.created(localizacao).body(PlanoAcaoMapper.paraResposta(plano));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um plano de ação ainda em aberto")
    public ResponseEntity<PlanoAcaoResposta> atualizar(
            @PathVariable Long id, @RequestBody @Valid AtualizacaoPlanoAcaoRequisicao requisicao) {
        return ResponseEntity.ok(PlanoAcaoMapper.paraResposta(planoAcaoService.atualizar(id, requisicao)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move o plano para outro status do ciclo de vida")
    public ResponseEntity<PlanoAcaoResposta> transicionarStatus(
            @PathVariable Long id, @RequestBody @Valid TransicaoStatusRequisicao requisicao) {
        return ResponseEntity.ok(
                PlanoAcaoMapper.paraResposta(planoAcaoService.transicionarStatus(id, requisicao)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um plano de ação")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        planoAcaoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
