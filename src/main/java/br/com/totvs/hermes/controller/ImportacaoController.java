package br.com.totvs.hermes.controller;

import br.com.totvs.hermes.dto.mapper.ImportacaoMapper;
import br.com.totvs.hermes.dto.mapper.ReuniaoMapper;
import br.com.totvs.hermes.dto.resposta.ImportacaoLoteResposta;
import br.com.totvs.hermes.dto.resposta.ResultadoImportacaoResposta;
import br.com.totvs.hermes.dto.resposta.ReuniaoResumoResposta;
import br.com.totvs.hermes.service.ImportacaoArquivoService;
import br.com.totvs.hermes.service.ImportacaoLoteService;
import br.com.totvs.hermes.service.ResultadoImportacao;
import br.com.totvs.hermes.service.ReuniaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Importação de transcrições em CSV e consulta dos lotes já processados.
 */
@RestController
@RequestMapping("/api/v1/importacoes")
@RequiredArgsConstructor
@Tag(name = "Importações", description = "Upload de CSV de transcrições e histórico de lotes")
public class ImportacaoController {

    private final ImportacaoArquivoService importacaoArquivoService;
    private final ImportacaoLoteService loteService;
    private final ReuniaoService reuniaoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa um CSV de transcrições, analisando cada reunião encontrada")
    public ResponseEntity<ResultadoImportacaoResposta> importar(
            @RequestParam("arquivo") MultipartFile arquivo) {

        ResultadoImportacao resultado = importacaoArquivoService.importar(arquivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ImportacaoMapper.paraResposta(resultado));
    }

    @GetMapping
    @Operation(summary = "Lista os lotes de importação, do mais recente para o mais antigo")
    public ResponseEntity<List<ImportacaoLoteResposta>> listar() {
        return ResponseEntity.ok(ImportacaoMapper.paraLista(loteService.listarTodos()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um lote de importação pelo identificador")
    public ResponseEntity<ImportacaoLoteResposta> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ImportacaoMapper.paraResposta(loteService.buscarPorId(id)));
    }

    @GetMapping("/{id}/reunioes")
    @Operation(summary = "Lista as reuniões importadas em um lote")
    public ResponseEntity<List<ReuniaoResumoResposta>> listarReunioes(@PathVariable Long id) {
        loteService.buscarPorId(id);
        return ResponseEntity.ok(ReuniaoMapper.paraLista(reuniaoService.listarPorLote(id)));
    }
}
