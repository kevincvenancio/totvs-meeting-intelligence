package br.com.totvs.insight360.controller;

import br.com.totvs.insight360.dto.filtro.FiltroReuniao;
import br.com.totvs.insight360.dto.mapper.AnaliseMapper;
import br.com.totvs.insight360.dto.mapper.PlanoAcaoMapper;
import br.com.totvs.insight360.dto.mapper.ReuniaoMapper;
import br.com.totvs.insight360.dto.requisicao.ReuniaoRequisicao;
import br.com.totvs.insight360.dto.requisicao.VinculoClienteRequisicao;
import br.com.totvs.insight360.dto.resposta.FeedbackResposta;
import br.com.totvs.insight360.dto.resposta.InsightResposta;
import br.com.totvs.insight360.dto.resposta.PaginaResposta;
import br.com.totvs.insight360.dto.resposta.PlanoAcaoResposta;
import br.com.totvs.insight360.dto.resposta.ReuniaoDetalheResposta;
import br.com.totvs.insight360.dto.resposta.ReuniaoResumoResposta;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.model.RiscoChurn;
import br.com.totvs.insight360.model.Sentimento;
import br.com.totvs.insight360.model.StatusCompletude;
import br.com.totvs.insight360.service.PlanoAcaoService;
import br.com.totvs.insight360.service.ReuniaoService;
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

/**
 * CRUD e consultas de reuniões — recurso central da API.
 */
@RestController
@RequestMapping("/api/v1/reunioes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reuniões", description = "Cadastro, análise e consulta das reuniões")
public class ReuniaoController {

    private final ReuniaoService reuniaoService;
    private final PlanoAcaoService planoAcaoService;

    // ── Consultas ─────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Lista reuniões de forma paginada, aplicando os filtros informados")
    public ResponseEntity<PaginaResposta<ReuniaoResumoResposta>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long loteId,
            @RequestParam(required = false) StatusCompletude statusCompletude,
            @RequestParam(required = false) Sentimento sentimento,
            @RequestParam(required = false) RiscoChurn riscoChurn,
            @RequestParam(required = false) String prioridade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Boolean somenteAnalisadas,
            @RequestParam(required = false) Boolean incluirDuplicadas,
            @RequestParam(required = false) String ordenarPor,
            @RequestParam(required = false) String direcao,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página mínima é 0.") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho mínimo da página é 1.")
            @Max(value = 100, message = "O tamanho máximo da página é 100.") int tamanho) {

        FiltroReuniao filtro = new FiltroReuniao(termo, clienteId, loteId, statusCompletude, sentimento,
                riscoChurn, prioridade, dataInicial, dataFinal, somenteAnalisadas, incluirDuplicadas,
                ordenarPor, direcao);

        List<ReuniaoResumoResposta> conteudo =
                ReuniaoMapper.paraLista(reuniaoService.pesquisar(filtro, pagina, tamanho));
        return ResponseEntity.ok(
                PaginaResposta.de(conteudo, pagina, tamanho, reuniaoService.contar(filtro)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca a análise completa de uma reunião")
    public ResponseEntity<ReuniaoDetalheResposta> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(montarDetalhe(reuniaoService.buscarDetalhada(id)));
    }

    @GetMapping("/{id}/insights")
    @Operation(summary = "Lista os insights extraídos da transcrição")
    public ResponseEntity<List<InsightResposta>> listarInsights(@PathVariable Long id) {
        return ResponseEntity.ok(AnaliseMapper.paraListaDeInsights(reuniaoService.listarInsights(id)));
    }

    @GetMapping("/{id}/feedback")
    @Operation(summary = "Consulta o feedback educativo gerado para a reunião")
    public ResponseEntity<FeedbackResposta> buscarFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(AnaliseMapper.paraResposta(reuniaoService.buscarFeedback(id)));
    }

    @GetMapping("/{id}/planos-acao")
    @Operation(summary = "Lista os planos de ação criados a partir da reunião")
    public ResponseEntity<List<PlanoAcaoResposta>> listarPlanosAcao(@PathVariable Long id) {
        reuniaoService.garantirExistencia(id);
        return ResponseEntity.ok(PlanoAcaoMapper.paraLista(planoAcaoService.listarPorReuniao(id)));
    }

    // ── Escrita ───────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Cadastra manualmente uma reunião e dispara a análise automática")
    public ResponseEntity<ReuniaoDetalheResposta> cadastrar(@RequestBody @Valid ReuniaoRequisicao requisicao) {
        Reuniao reuniao = reuniaoService.cadastrar(requisicao);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(reuniao.getId())
                .toUri();
        return ResponseEntity.created(localizacao).body(montarDetalhe(reuniao));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma reunião, reanalisando quando a transcrição muda")
    public ResponseEntity<ReuniaoDetalheResposta> atualizar(@PathVariable Long id,
                                                            @RequestBody @Valid ReuniaoRequisicao requisicao) {
        return ResponseEntity.ok(montarDetalhe(reuniaoService.atualizar(id, requisicao)));
    }

    @PostMapping("/{id}/analise")
    @Operation(summary = "Executa novamente a análise automática da transcrição")
    public ResponseEntity<ReuniaoDetalheResposta> reanalisar(@PathVariable Long id) {
        return ResponseEntity.ok(montarDetalhe(reuniaoService.reanalisar(id)));
    }

    @PutMapping("/{id}/cliente")
    @Operation(summary = "Vincula a reunião a um cliente da carteira")
    public ResponseEntity<ReuniaoDetalheResposta> vincularCliente(
            @PathVariable Long id, @RequestBody @Valid VinculoClienteRequisicao requisicao) {
        reuniaoService.vincularCliente(id, requisicao.clienteId());
        return ResponseEntity.ok(montarDetalhe(reuniaoService.buscarDetalhada(id)));
    }

    @DeleteMapping("/{id}/cliente")
    @Operation(summary = "Desfaz o vínculo da reunião com o cliente")
    public ResponseEntity<ReuniaoDetalheResposta> desvincularCliente(@PathVariable Long id) {
        reuniaoService.desvincularCliente(id);
        return ResponseEntity.ok(montarDetalhe(reuniaoService.buscarDetalhada(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a reunião e todo o conteúdo derivado dela")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        reuniaoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    // ── Apoio ─────────────────────────────────────────────────────────

    /** Monta o detalhe da reunião somando os contadores exibidos na tela. */
    private ReuniaoDetalheResposta montarDetalhe(Reuniao reuniao) {
        Long id = reuniao.getId();
        return ReuniaoMapper.paraDetalhe(reuniao,
                reuniaoService.contarInsights(id),
                reuniaoService.contarComentarios(id),
                reuniaoService.contarPlanosAcao(id));
    }
}
