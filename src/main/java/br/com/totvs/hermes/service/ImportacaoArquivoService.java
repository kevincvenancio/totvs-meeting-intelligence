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

/**
 * Recebe o CSV enviado pelo Front-End e delega o processamento ao
 * {@link CsvService}, que já é usado pela CLI.
 * <p>
 * O arquivo é gravado em um diretório temporário porque o processamento trabalha
 * com {@link File}; ao final, o temporário é sempre apagado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacaoArquivoService {

    private static final String EXTENSAO_ESPERADA = ".csv";
    private static final String PREFIXO_TEMPORARIO = "hermes-importacao-";

    private final CsvService csvService;

    /**
     * Importa um CSV de transcrições enviado por upload.
     *
     * @param arquivoEnviado arquivo multipart recebido do Front-End
     * @return resumo da importação, com o lote criado e os totais processados
     * @throws ArquivoInvalidoException quando o arquivo está vazio, não é CSV ou não pôde ser lido
     */
    public ResultadoImportacao importar(MultipartFile arquivoEnviado) {
        validar(arquivoEnviado);
        Path diretorioTemporario = null;
        try {
            diretorioTemporario = Files.createTempDirectory(PREFIXO_TEMPORARIO);
            Path arquivo = gravarTemporario(arquivoEnviado, diretorioTemporario);
            ResultadoImportacao resultado = csvService.processar(arquivo.toFile());
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
            throw new ArquivoInvalidoException("Envie um arquivo CSV não vazio no campo 'arquivo'.");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase().endsWith(EXTENSAO_ESPERADA)) {
            throw new ArquivoInvalidoException("O arquivo enviado precisa ter a extensão .csv.");
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
        return nome.isBlank() ? "importacao" + EXTENSAO_ESPERADA : nome;
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
