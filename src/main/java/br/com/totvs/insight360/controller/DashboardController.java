package br.com.totvs.insight360.controller;

import br.com.totvs.insight360.dto.resposta.DashboardResposta;
import br.com.totvs.insight360.service.PainelExecutivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Indicadores consolidados do painel executivo.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores consolidados para o painel executivo")
public class DashboardController {

    private final PainelExecutivoService painelExecutivoService;

    @GetMapping
    @Operation(summary = "Retorna todos os indicadores do painel executivo")
    public ResponseEntity<DashboardResposta> obterPainel() {
        return ResponseEntity.ok(painelExecutivoService.montarPainel());
    }
}
