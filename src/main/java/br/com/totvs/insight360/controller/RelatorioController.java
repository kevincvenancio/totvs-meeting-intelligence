package br.com.totvs.insight360.controller;

import br.com.totvs.insight360.service.ArquivoRelatorio;
import br.com.totvs.insight360.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Download dos relatórios executivos em PDF.
 */
@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Geração e download dos relatórios em PDF")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/executivo")
    @Operation(summary = "Gera o relatório executivo com todas as reuniões analisadas")
    public ResponseEntity<byte[]> gerarExecutivo() {
        return responderComPdf(relatorioService.gerarGeral());
    }

    @GetMapping("/reunioes/{id}")
    @Operation(summary = "Gera o relatório individual de uma reunião")
    public ResponseEntity<byte[]> gerarDaReuniao(@PathVariable Long id) {
        return responderComPdf(relatorioService.gerarDaReuniao(id));
    }

    @GetMapping("/lotes/{id}")
    @Operation(summary = "Gera o relatório executivo de um lote de importação")
    public ResponseEntity<byte[]> gerarDoLote(@PathVariable Long id) {
        return responderComPdf(relatorioService.gerarDoLote(id));
    }

    /** Monta a resposta binária com os cabeçalhos de download. */
    private ResponseEntity<byte[]> responderComPdf(ArquivoRelatorio relatorio) {
        ContentDisposition disposicao = ContentDisposition.attachment()
                .filename(relatorio.nomeArquivo(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(relatorio.tamanho())
                .body(relatorio.conteudo());
    }
}
