package br.com.totvs.hermes.service;

import br.com.totvs.hermes.exception.RegraNegocioException;
import br.com.totvs.hermes.exception.RelatorioException;
import br.com.totvs.hermes.model.ImportacaoLote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Disponibiliza os relatórios em PDF para download pela API.
 * <p>
 * O {@link PdfService} grava o arquivo em disco; aqui ele é lido para memória e
 * as falhas técnicas são traduzidas em {@link RelatorioException}, deixando os
 * controllers livres de {@code try/catch}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelatorioService {

    private final PdfService pdfService;
    private final ReuniaoService reuniaoService;
    private final ImportacaoLoteService loteService;

    @Value("${hermes.relatorios.diretorio:./relatorios}")
    private String diretorioRelatorios;

    /** Gera o relatório individual de uma reunião. */
    public ArquivoRelatorio gerarDaReuniao(Long reuniaoId) {
        reuniaoService.garantirExistencia(reuniaoId);
        return gerar(() -> pdfService.gerarPdfReuniao(reuniaoId, prepararDiretorio()),
                "relatório da reunião #" + reuniaoId);
    }

    /** Gera o relatório executivo consolidado de todas as reuniões analisadas. */
    public ArquivoRelatorio gerarGeral() {
        if (reuniaoService.contarAnalisadas() == 0) {
            throw new RegraNegocioException(
                    "Não há reuniões analisadas para compor o relatório executivo.");
        }
        return gerar(() -> pdfService.gerarPdfGeral(prepararDiretorio()), "relatório executivo");
    }

    /** Gera o relatório executivo de um lote de importação. */
    public ArquivoRelatorio gerarDoLote(Long loteId) {
        ImportacaoLote lote = loteService.buscarPorId(loteId);
        if (reuniaoService.contarPorLote(loteId) == 0) {
            throw new RegraNegocioException(
                    "O lote " + lote.getCodigoLote() + " não possui reuniões para compor o relatório.");
        }
        return gerar(() -> pdfService.gerarPdfPorLote(lote, prepararDiretorio()),
                "relatório do lote " + lote.getCodigoLote());
    }

    // ── Apoio ─────────────────────────────────────────────────────────

    /** Contrato interno para uniformizar a chamada dos geradores do {@link PdfService}. */
    @FunctionalInterface
    private interface GeradorPdf {
        File gerar() throws Exception;
    }

    private ArquivoRelatorio gerar(GeradorPdf gerador, String descricao) {
        try {
            File arquivo = gerador.gerar();
            byte[] conteudo = Files.readAllBytes(arquivo.toPath());
            log.info("PDF gerado: {} ({} bytes)", arquivo.getName(), conteudo.length);
            return new ArquivoRelatorio(arquivo.getName(), conteudo);
        } catch (IllegalArgumentException excecao) {
            throw new RegraNegocioException(excecao.getMessage());
        } catch (Exception excecao) {
            throw new RelatorioException("Não foi possível gerar o " + descricao + ".", excecao);
        }
    }

    /** Garante a existência do diretório de saída configurado. */
    private String prepararDiretorio() throws IOException {
        Path diretorio = Path.of(diretorioRelatorios);
        Files.createDirectories(diretorio);
        return diretorio.toAbsolutePath().toString();
    }
}
