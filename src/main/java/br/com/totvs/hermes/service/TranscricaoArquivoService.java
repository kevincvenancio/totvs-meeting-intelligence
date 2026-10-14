package br.com.totvs.hermes.service;

import br.com.totvs.hermes.model.ImportacaoLote;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.repository.ReuniaoRepository;
import br.com.totvs.hermes.util.ExtratorTextoDocumento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importa um documento avulso (.txt ou .docx) contendo a transcrição de UMA reunião.
 * <p>
 * O conteúdo do arquivo é a transcrição. Opcionalmente, o documento pode começar
 * com um cabeçalho delimitado por linhas {@code ---}, no formato "Chave: valor":
 * <pre>
 * ---
 * Cliente: Construtora Alfa
 * Data: 2025-03-10
 * Vendedor: T12345
 * Duração: 45min
 * ---
 * Vendedor: Bom dia, tudo bem?
 * ...
 * </pre>
 * Sem cabeçalho, o cliente vem do nome do arquivo e a data, se houver uma no
 * nome (ex.: {@code acme_2025-03-10.txt}). O identificador externo, quando não
 * informado, é derivado do conteúdo — reimportar o mesmo texto cai como duplicada.
 * <p>
 * A reunião é gravada e analisada pelo mesmo caminho das linhas do CSV
 * ({@link CsvService#salvarReuniao}), então o resultado no painel é idêntico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscricaoArquivoService {

    private static final String DELIMITADOR_CABECALHO = "---";
    private static final int MAX_LINHAS_CABECALHO = 30;

    private static final Pattern DATA_ISO = Pattern.compile("(\\d{4})[-_.](\\d{2})[-_.](\\d{2})");
    private static final Pattern DATA_BR  = Pattern.compile("(\\d{2})[-_.](\\d{2})[-_.](\\d{4})");

    /** Chaves aceitas no cabeçalho (sem acento, minúsculas) → campo equivalente do CSV. */
    private static final Map<String, String> CHAVES_CABECALHO = Map.ofEntries(
            Map.entry("id",          "ID_MEETING"),
            Map.entry("idmeeting",   "ID_MEETING"),
            Map.entry("idreuniao",   "ID_MEETING"),
            Map.entry("cliente",     "NOME_UNIDADE"),
            Map.entry("unidade",     "NOME_UNIDADE"),
            Map.entry("empresa",     "NOME_UNIDADE"),
            Map.entry("data",        "DT_MEETING"),
            Map.entry("vendedor",    "CODT"),
            Map.entry("codt",        "CODT"),
            Map.entry("duracao",     "DURACAO_MEETING"),
            Map.entry("formato",     "FORMATO_MEETING"),
            Map.entry("status",      "STATUS_MEETING"),
            Map.entry("uf",          "UF"),
            Map.entry("segmento",    "NOME_SEGMENTO"),
            Map.entry("cnae",        "CNAE"),
            Map.entry("faturamento", "FAIXA_FATURAMENTO_CLIENTE_EC"),
            Map.entry("nps",         "NOTA_NPS"),
            Map.entry("notanps",     "NOTA_NPS")
    );

    private final CsvService          csvService;
    private final ImportacaoLoteService loteService;
    private final DuplicidadeService  duplicidadeService;
    private final ReuniaoRepository   reuniaoRepository;

    @Transactional
    public ResultadoImportacao processar(File arquivo) throws IOException {
        // Extrai antes de criar o lote: um .docx corrompido não deixa lote órfão.
        String conteudo = ExtratorTextoDocumento.extrair(arquivo);
        Map<String, String> campos = montarCampos(conteudo, arquivo.getName());

        ImportacaoLote lote = loteService.criarLote(arquivo);
        log.info("Transcricao avulsa lida: {} caracteres | lote: {}", conteudo.length(), lote.getCodigoLote());

        int validas = 0, incompletas = 0, duplicadas = 0, erros = 0;
        List<String> erroDetalhe = new ArrayList<>();

        if (campos.get("ANON_TRANSCRICAO").isBlank()) {
            incompletas = 1;
        } else {
            try {
                Reuniao salva = csvService.salvarReuniao(campos, lote, arquivo.getName());
                if (duplicidadeService.verificarEMarcar(salva)) {
                    duplicadas = 1;
                    reuniaoRepository.save(salva);
                } else {
                    validas = 1;
                }
            } catch (Exception ex) {
                erros = 1;
                erroDetalhe.add(arquivo.getName() + ": " + ex.getMessage());
                log.warn("Erro ao importar transcricao {}: {}", arquivo.getName(), ex.getMessage());
            }
        }

        String obs = erroDetalhe.isEmpty() ? null : "Erros: " + erroDetalhe.get(0);
        loteService.finalizarLote(lote, 1, validas, incompletas, duplicadas, erros, obs);
        return new ResultadoImportacao(lote, validas, incompletas, duplicadas, erros, 1, erroDetalhe);
    }

    /**
     * Separa o cabeçalho opcional da transcrição e devolve os campos com as
     * mesmas chaves usadas pelo {@link CsvService}.
     */
    static Map<String, String> montarCampos(String conteudo, String nomeArquivo) {
        String texto = conteudo.replace("\r\n", "\n").replace("\r", "\n");
        Map<String, String> campos = new LinkedHashMap<>();

        String[] linhas = texto.split("\n", -1);
        int inicio = 0;
        while (inicio < linhas.length && linhas[inicio].isBlank()) inicio++;

        if (inicio < linhas.length && linhas[inicio].trim().equals(DELIMITADOR_CABECALHO)) {
            int fim = -1;
            for (int i = inicio + 1; i < linhas.length && i <= inicio + MAX_LINHAS_CABECALHO; i++) {
                if (linhas[i].trim().equals(DELIMITADOR_CABECALHO)) { fim = i; break; }
            }
            if (fim > 0) {
                for (int i = inicio + 1; i < fim; i++) lerLinhaCabecalho(linhas[i], campos);
                texto = String.join("\n", Arrays.copyOfRange(linhas, fim + 1, linhas.length));
            }
        }

        String transcricao = texto.strip();
        campos.put("ANON_TRANSCRICAO", transcricao);

        String base = nomeSemExtensao(nomeArquivo);
        campos.computeIfAbsent("DT_MEETING", k -> dataNoNome(base));
        campos.computeIfAbsent("NOME_UNIDADE", k -> clienteNoNome(base));
        campos.computeIfAbsent("ID_MEETING", k -> transcricao.isEmpty() ? null : idPorConteudo(transcricao));
        return campos;
    }

    private static void lerLinhaCabecalho(String linha, Map<String, String> campos) {
        int separador = linha.indexOf(':');
        if (separador <= 0) return;
        String chave = CHAVES_CABECALHO.get(normalizarChave(linha.substring(0, separador)));
        String valor = linha.substring(separador + 1).trim();
        if (chave != null && !valor.isEmpty()) campos.put(chave, valor);
    }

    private static String normalizarChave(String chave) {
        return Normalizer.normalize(chave, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z]", "");
    }

    private static String nomeSemExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        return ponto > 0 ? nomeArquivo.substring(0, ponto) : nomeArquivo;
    }

    /** Data no nome do arquivo, no formato que o {@link CsvService} entende. */
    private static String dataNoNome(String base) {
        Matcher iso = DATA_ISO.matcher(base);
        if (iso.find()) return iso.group(1) + "-" + iso.group(2) + "-" + iso.group(3);
        Matcher br = DATA_BR.matcher(base);
        if (br.find()) return br.group(1) + "/" + br.group(2) + "/" + br.group(3);
        return null;
    }

    /** Nome do arquivo sem a data e sem separadores, usado como nome do cliente. */
    private static String clienteNoNome(String base) {
        String nome = DATA_BR.matcher(DATA_ISO.matcher(base).replaceAll(" ")).replaceAll(" ")
                .replaceAll("[_]+", " ")
                .replaceAll("^[\\s\\-.]+|[\\s\\-.]+$", "")
                .replaceAll("\\s{2,}", " ");
        return nome.isBlank() ? null : nome;
    }

    /** Identificador estável derivado do texto, para detectar reimportações do mesmo documento. */
    private static String idPorConteudo(String transcricao) {
        try {
            String normalizado = transcricao.replaceAll("\\s+", " ").toLowerCase();
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(normalizado.getBytes(StandardCharsets.UTF_8));
            return "ARQ-" + HexFormat.of().withUpperCase().formatHex(hash, 0, 5);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
