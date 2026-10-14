package br.com.totvs.hermes.service;

import br.com.totvs.hermes.exception.ArquivoInvalidoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Recebe o arquivo enviado pelo Front-End e delega o processamento conforme a
 * extensão: CSV de transcrições vai para o {@link CsvService} (o mesmo da CLI);
 * documentos .txt e .docx, com a transcrição de uma reunião, vão para o
 * {@link TranscricaoArquivoService}.
 * <p>
 * O arquivo é gravado em um diretório temporário porque o processamento trabalha
 * com {@link File}; ao final, o temporário é sempre apagado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacaoArquivoService {

    private static final String EXTENSAO_CSV = ".csv";
    private static final List<String> EXTENSOES_ACEITAS = List.of(EXTENSAO_CSV, ".txt", ".docx");
    private static final String PREFIXO_TEMPORARIO = "hermes-importacao-";

    private final CsvService csvService;
    private final TranscricaoArquivoService transcricaoArquivoService;

    /**
     * Importa um CSV de transcrições ou um documento (.txt/.docx) com uma transcrição.
     *
     * @param arquivoEnviado arquivo multipart recebido do Front-End
     * @return resumo da importação, com o lote criado e os totais processados
     * @throws ArquivoInvalidoException quando o arquivo está vazio, tem extensão não suportada ou não pôde ser lido
     */
    public ResultadoImportacao importar(MultipartFile arquivoEnviado) {
        validar(arquivoEnviado);
        Path diretorioTemporario = null;
        try {
            diretorioTemporario = Files.createTempDirectory(PREFIXO_TEMPORARIO);
            Path arquivo = gravarTemporario(arquivoEnviado, diretorioTemporario);
            boolean csv = arquivo.getFileName().toString().toLowerCase().endsWith(EXTENSAO_CSV);
            ResultadoImportacao resultado = csv
                    ? csvService.processar(arquivo.toFile())
                    : transcricaoArquivoService.processar(arquivo.toFile());
            log.info("Importacao via API concluida: {} valida(s), {} duplicada(s), {} erro(s)",
                    resultado.processadas(), resultado.duplicadas(), resultado.erros());
            return resultado;
        } catch (IOException excecao) {
            throw new ArquivoInvalidoException(
                    "Não foi possível ler o arquivo enviado: " + excecao.getMessage(), excecao);
        } finally {
            remover(diretorioTemporario);
        }
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Envie um arquivo não vazio no campo 'arquivo'.");
        }
        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase();
        if (nome.endsWith(".doc")) {
            throw new ArquivoInvalidoException(
                    "Arquivos .doc (Word 97-2003) não são suportados. Salve o documento como .docx.");
        }
        if (EXTENSOES_ACEITAS.stream().noneMatch(nome::endsWith)) {
            throw new ArquivoInvalidoException("O arquivo enviado precisa ter a extensão .csv, .txt ou .docx.");
        }
    }

    /**
     * Grava o arquivo enviado dentro do diretório temporário preservando o nome
     * original, que é o nome registrado no lote de importação.
     */
    private Path gravarTemporario(MultipartFile arquivo, Path diretorio) throws IOException {
        Path destino = diretorio.resolve(nomeSeguro(arquivo.getOriginalFilename()));
        arquivo.transferTo(destino);
        return destino;
    }

    /** Descarta qualquer caminho embutido no nome enviado pelo cliente. */
    private String nomeSeguro(String nomeOriginal) {
        String nome = nomeOriginal == null ? "" : nomeOriginal.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return nome.isBlank() ? "importacao" + EXTENSAO_CSV : nome;
    }

    /** Remove o diretório temporário e o arquivo gravado dentro dele. */
    private void remover(Path diretorio) {
        if (diretorio == null) {
            return;
        }
        try (var conteudo = Files.walk(diretorio)) {
            conteudo.sorted(Comparator.reverseOrder()).forEach(caminho -> {
                try {
                    Files.deleteIfExists(caminho);
                } catch (IOException excecao) {
                    log.warn("Nao foi possivel remover {}: {}", caminho, excecao.getMessage());
                }
            });
        } catch (IOException excecao) {
            log.warn("Nao foi possivel limpar o diretorio temporario {}: {}", diretorio, excecao.getMessage());
        }
    }
}
