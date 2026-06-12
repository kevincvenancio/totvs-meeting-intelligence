package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.ImportacaoLote;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.ReuniaoRepository;
import br.com.totvs.insight360.util.TextoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvService {

    private final ReuniaoRepository         reuniaoRepository;
    private final AnaliseTranscricaoService  analiseService;
    private final FeedbackReuniaoService     feedbackService;
    private final ImportacaoLoteService      loteService;
    private final DuplicidadeService         duplicidadeService;

    private static final Pattern REC_START = Pattern.compile(
            "(?m)^\"(\\d+),(\\d{4}-\\d{2}-\\d{2})");

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    };


    // todo o processamento do arquivo. Sem isso, criarLote() abria e

    @Transactional
    public ResultadoImportacao processar(File arquivo) throws IOException {
        ImportacaoLote lote = loteService.criarLote(arquivo);
        String conteudo = lerArquivo(arquivo);

        Matcher m = REC_START.matcher(conteudo);
        List<Integer> starts = new ArrayList<>();
        while (m.find()) starts.add(m.start());

        log.info("CSV lido: {} bytes | {} registros encontrados | lote: {}", conteudo.length(), starts.size(), lote.getCodigoLote());

        int validas = 0, erros = 0, incompletas = 0, duplicadas = 0;
        List<String> erroDetalhe = new ArrayList<>();
        Set<String> idsNesteLote = new HashSet<>();

        for (int i = 0; i < starts.size(); i++) {
            int s   = starts.get(i);
            int e   = (i + 1 < starts.size()) ? starts.get(i + 1) : conteudo.length();
            String raw = conteudo.substring(s, e).strip();
            try {
                Map<String, String> campos = parseRegistro(raw);
                if (campos == null) { erros++; erroDetalhe.add("Reg " + (i+1) + ": falha no parsing"); continue; }

                String transcricao = campos.get("ANON_TRANSCRICAO");
                if (transcricao == null || transcricao.isBlank()) { incompletas++; continue; }

                String idExterno = campos.get("ID_MEETING");
                if (idExterno != null && !idExterno.isBlank() && !idsNesteLote.add(idExterno)) {
                    duplicadas++; continue;
                }

                Reuniao salva = salvarReuniao(campos, lote, arquivo.getName());
                if (duplicidadeService.verificarEMarcar(salva)) {
                    duplicadas++;
                    reuniaoRepository.save(salva);
                } else {
                    validas++;
                }
            } catch (Exception ex) {
                erros++;
                erroDetalhe.add("Reg " + (i+1) + ": " + ex.getMessage());
                log.warn("Erro no registro {}: {}", i+1, ex.getMessage());
            }
        }

        String obs = erroDetalhe.isEmpty() ? null : "Erros: " + String.join("; ", erroDetalhe.subList(0, Math.min(5, erroDetalhe.size())));
        loteService.finalizarLote(lote, starts.size(), validas, incompletas, duplicadas, erros, obs);

        return new ResultadoImportacao(
                lote, validas, incompletas, duplicadas, erros,
                starts.size(),
                erroDetalhe.subList(0, Math.min(10, erroDetalhe.size()))
        );
    }

    private Map<String, String> parseRegistro(String raw) {
        if (raw.startsWith("\"") && raw.endsWith("\"")) raw = raw.substring(1, raw.length() - 1);
        String[] p10 = raw.split(",", 11);
        if (p10.length < 11) return null;
        String idMeeting = p10[0].trim(); if (!idMeeting.matches("\\d+")) return null;
        String rest = p10[10];
        int sepIdx = rest.lastIndexOf("\"\"");
        if (sepIdx < 0) return null;
        String transRaw = rest.substring(0, sepIdx);
        if (transRaw.startsWith("\"\"")) transRaw = transRaw.substring(2);
        String transcricao = transRaw.replace("\"\"", "\"").replace("\r\n", "\n").replace("\r", "\n").trim();
        String tail = rest.substring(sepIdx + 2);
        if (tail.startsWith(",")) tail = tail.substring(1);
        String[] tf = tail.split(",", 7);
        String uf = tf.length > 0 ? tf[0].trim() : "";
        if (uf.length() > 2 || uf.contains("[")) uf = "";
        String duracao = p10[5].trim();
        if (!duracao.matches("[\\d:hHmM ]+")) duracao = "";
        Map<String, String> c = new LinkedHashMap<>();
        c.put("ID_MEETING",                   idMeeting);
        c.put("DT_MEETING",                   p10[1].trim());
        c.put("FORMATO_MEETING",              p10[2].trim());
        c.put("STATUS_MEETING",               p10[4].trim());
        c.put("DURACAO_MEETING",              duracao);
        c.put("CODT",                         p10[6].trim());
        c.put("TP_RECURSO",                   p10[7].trim());
        c.put("FLG_EXTERNO",                  p10[8].trim());
        c.put("ANON_TRANSCRICAO",             transcricao);
        c.put("UF",                           uf);
        c.put("CNAE",                         tf.length > 1 ? tf[1].trim() : "");
        c.put("NOME_UNIDADE",                 tf.length > 2 ? tf[2].trim() : "");
        c.put("NOME_SEGMENTO",                tf.length > 3 ? tf[3].trim() : "");
        c.put("FAIXA_FATURAMENTO_CLIENTE_EC", tf.length > 4 ? tf[4].trim() : "");
        c.put("DT_ULTIMA_PESQUISA",           tf.length > 5 ? tf[5].trim() : "");
        c.put("NOTA_NPS",                     tf.length > 6 ? tf[6].trim().replace("\"","") : "");
        return c;
    }

    // ── CORREÇÃO: @Transactional REMOVIDO daqui. Como este método é chamado
    // internamente por processar(), o proxy do Spring nunca interceptaria
    // esta anotação de qualquer forma (self-invocation). A transação agora
    // é herdada de processar(), que é o ponto de entrada correto.
    public Reuniao salvarReuniao(Map<String, String> c, ImportacaoLote lote, String nomeArquivo) {
        Reuniao r = new Reuniao();
        r.setIdExterno(c.get("ID_MEETING"));
        r.setData(parseData(c.get("DT_MEETING")));
        r.setDuracaoMinutos(parseDuracao(c.get("DURACAO_MEETING")));
        r.setDuracaoFormatada(formatarDuracao(c.get("DURACAO_MEETING")));
        r.setFormato(c.get("FORMATO_MEETING"));
        r.setStatusMeetingOriginal(c.get("STATUS_MEETING"));
        r.setVendedor(c.get("CODT"));
        r.setTpRecurso(c.get("TP_RECURSO"));
        r.setFlgExterno("true".equalsIgnoreCase(c.get("FLG_EXTERNO")));
        r.setUf(c.get("UF"));
        r.setCnae(c.get("CNAE"));
        r.setNomeUnidade(c.get("NOME_UNIDADE"));
        r.setSegmento(c.get("NOME_SEGMENTO"));
        r.setFaturamento(c.get("FAIXA_FATURAMENTO_CLIENTE_EC"));
        r.setDtUltimaPesquisa(parseData(c.get("DT_ULTIMA_PESQUISA")));
        String npsStr = c.get("NOTA_NPS");
        if (npsStr != null && !npsStr.isBlank()) { try { r.setNotaNps(Double.parseDouble(npsStr)); } catch (NumberFormatException ignored) {} }
        String unidade = c.get("NOME_UNIDADE");
        r.setCliente(unidade != null && !unidade.isBlank() ? unidade : "Cliente " + c.get("ID_MEETING"));
        String transcricao = c.get("ANON_TRANSCRICAO");
        r.setTranscricaoOriginal(transcricao);
        r.setTranscricaoTratada(TextoUtils.tratarTexto(transcricao));
        r.setAnalisada(false);
        r.setLoteImportacao(lote);
        r.setNomeArquivoOrigem(nomeArquivo);
        r.setDuplicada(false);
        Reuniao salva = reuniaoRepository.save(r);
        analiseService.analisarReuniao(salva);
        feedbackService.gerarFeedback(salva);
        return salva;
    }

    private String lerArquivo(File arquivo) throws IOException {
        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        for (String charset : new String[]{"UTF-8", "ISO-8859-1", "windows-1252"}) {
            try {
                String s = new String(bytes, Charset.forName(charset));
                if (!s.contains("\uFFFD")) return s;
            } catch (java.nio.charset.UnsupportedCharsetException ignored) {}
        }
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    public static int parseDuracao(String s) {
        if (s == null || s.isBlank()) return 0;
        s = s.trim();
        if (s.matches("\\d{1,2}:\\d{2}:\\d{2}")) { String[] p = s.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); }
        if (s.matches("\\d{1,2}:\\d{2}"))         { String[] p = s.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); }
        Matcher mh = Pattern.compile("(\\d+)\\s*[hH]\\s*(\\d+)?\\s*[mM]?").matcher(s);
        if (mh.find()) { int h = Integer.parseInt(mh.group(1)); int mi = mh.group(2) != null ? Integer.parseInt(mh.group(2)) : 0; return h * 60 + mi; }
        Matcher mm = Pattern.compile("(\\d+)\\s*[mM]").matcher(s);
        if (mm.find()) return Integer.parseInt(mm.group(1));
        try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        return 0;
    }

    public static String formatarDuracao(String s) {
        int min = parseDuracao(s); if (min <= 0) return "Não informado";
        int h = min / 60; int mi = min % 60;
        if (h > 0 && mi > 0) return h + "h " + mi + "min";
        if (h > 0)           return h + "h";
        return mi + "min";
    }

    private LocalDate parseData(String s) {
        if (s == null || s.isBlank()) return null;
        String d = s.trim().split(" ")[0].split("T")[0];
        for (DateTimeFormatter f : DATE_FORMATTERS) { try { return LocalDate.parse(d, f); } catch (DateTimeParseException ignored) {} }
        return null;
    }
}