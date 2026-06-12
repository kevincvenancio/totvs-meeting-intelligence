package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.FeedbackReuniao;
import br.com.totvs.insight360.model.ImportacaoLote;
import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.InsightRepository;
import br.com.totvs.insight360.repository.ReuniaoRepository;
import br.com.totvs.insight360.util.TextoUtils;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servico de geracao de relatorios PDF executivos — TOTVS Insight360.
 */
@Service
@RequiredArgsConstructor
public class PdfService {

    private final ReuniaoRepository reuniaoRepo;
    private final InsightRepository insightRepo;
    private final FeedbackReuniaoService feedbackService;
    private final DashboardService dashboardService;
    private final ChartService chartService;


    // ── Paleta de cores TOTVS ──────────────────────────────────────────
    private static final Color DARK    = new Color(0,   20,  60);
    private static final Color BLUE    = new Color(0,   87,  255);
    private static final Color CYAN    = new Color(0,   180, 230);
    private static final Color RED     = new Color(232, 56,  79);
    private static final Color LIGHT   = new Color(240, 245, 255);
    private static final Color GRAY    = new Color(220, 226, 235);
    private static final Color GREEN   = new Color(16,  185, 129);
    private static final Color ORANGE  = new Color(245, 158, 11);
    private static final Color WHITE   = new Color(255, 255, 255);
    private static final Color TDARK   = new Color(15,  23,  42);
    private static final Color TMUTED  = new Color(71,  85,  105);

    private static final Color BG_GREEN  = new Color(209, 250, 229);
    private static final Color BG_YELLOW = new Color(254, 243, 199);
    private static final Color BG_RED    = new Color(254, 226, 226);
    private static final Color BG_BLUE   = new Color(219, 234, 254);

    private static final DateTimeFormatter FMT    = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DH = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Mensagem fixa usada em alerta de locutores nao identificados
    private static final String MSG_LOCUTORES_AUSENTES =
            "Locutores nao identificados na transcricao desta reuniao.";

    // ── Limites do relatorio executivo (mantem o PDF sucinto: ~10-12 paginas) ──
    // O detalhamento exibe apenas as reunioes mais prioritarias; as demais
    // ficam disponiveis via PDF individual (menu) ou exportacao CSV.
    private static final int LIMITE_TABELA_DETALHE     = 25; // linhas na tabela-resumo
    private static final int LIMITE_CARDS_PRIORITARIOS = 8;  // fichas detalhadas
    private static final int LIMITE_CARDS_INCOMPLETAS  = 4;  // fichas de incompletas
    private static final int ALTURA_MAX_GRAFICO        = 170; // px (antes 220)

    // ══════════════════════════════════════════════════════════════════
    //  RELATORIO INDIVIDUAL DE REUNIAO
    // ══════════════════════════════════════════════════════════════════

    public File gerarPdfReuniao(Long id, String pasta) throws Exception {
        Reuniao r          = reuniaoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reuniao nao encontrada: " + id));
        java.util.List<Insight> insights = insightRepo.findByReuniaoId(id);
        Optional<FeedbackReuniao> fb     = feedbackService.buscarPorReuniao(id);

        Document doc = new Document(PageSize.A4, 50, 50, 80, 60);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter w = PdfWriter.getInstance(doc, baos);
        w.setPageEvent(rodape("TOTVS Insight360  |  Relatorio Individual — Reuniao #" + r.getId()));
        doc.open();

        gerarCapaReuniao(doc, w, r);
        doc.newPage();
        gerarResumoExecutivoReuniao(doc, r);
        doc.newPage();
        gerarDiagnosticoComercial(doc, r);
        doc.newPage();
        gerarScores(doc, r);
        doc.newPage();
        gerarParticipantes(doc, r);
        doc.newPage();
        gerarInsights(doc, insights);
        doc.newPage();
        gerarProximosPassos(doc, r);
        doc.newPage();
        gerarFeedbackEducativo(doc, r, fb.orElse(null));
        doc.newPage();
        gerarAnexosReuniao(doc);

        doc.close();

        File out = new File(pasta, "insight360_reuniao_" + id + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(baos.toByteArray()); }
        return out;
    }

    // ── P1: Capa da Reuniao ───────────────────────────────────────────
    private void gerarCapaReuniao(Document doc, PdfWriter w, Reuniao r)
            throws DocumentException {
        PdfContentByte cb = w.getDirectContent();
        cb.setColorFill(DARK);
        cb.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
        cb.fill();
        cb.setColorFill(BLUE);
        cb.rectangle(0, 0, 12, PageSize.A4.getHeight());
        cb.fill();
        cb.setColorFill(new Color(0, 57, 150));
        cb.rectangle(12, 200, PageSize.A4.getWidth() - 12, 2);
        cb.fill();

        PdfPTable t = tbl(1);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(160);

        // FIX: hardcoded "TOTVS" e "Insight360" diretamente — parte1/parte2 eram sempre esses valores
        celulaLogo(t);
        celulaSemBorda(t, "Relatorio Individual de Inteligencia Comercial",
                fn(13, new Color(148, 163, 184)), 10, 20);
        celulaDivisor(t, BLUE);
        celulaSemBorda(t, "Reuniao #" + r.getId() + "  —  " + s(r.getCliente()),
                fb(11, CYAN), 10, 6);
        celulaSemBorda(t, "Data: " + (r.getData() != null ? r.getData().format(FMT) : "Nao informada")
                        + "  |  Duracao: " + r.getDuracaoDisplay()
                        + "  |  Status: " + s(r.getStatusCompletude() != null ? r.getStatusCompletude().name() : null),
                fn(9, new Color(100, 116, 139)), 4, 6);
        celulaSemBorda(t, "Gerado em " + LocalDate.now().format(FMT),
                fn(8, new Color(71, 85, 105)), 20, 0);

        doc.add(t);
        doc.newPage();
    }

    // ── P2: Resumo Executivo ──────────────────────────────────────────
    private void gerarResumoExecutivoReuniao(Document doc, Reuniao r)
            throws DocumentException {
        secTitulo(doc, "Resumo Executivo da Reuniao");

        PdfPTable cards1 = tbl(4);
        criarCard(cards1, "Cliente / Unidade",    s(r.getCliente()));
        criarCard(cards1, "Data",                 r.getData() != null ? r.getData().format(FMT) : "—");
        criarCard(cards1, "Duracao",              r.getDuracaoDisplay());
        criarCard(cards1, "Categoria",            s(r.getCategoriaPrincipal()));
        doc.add(cards1); br(doc);

        PdfPTable cards2 = tbl(4);
        criarCardComCor(cards2, "Sentimento",       r.getSentimentoFormatado(),   corSentimento(r.getSentimento()));
        criarCardComCor(cards2, "Score Qualidade",  scoreDisplay(r.getScoreQualidade()), corScore(r.getScoreQualidade()));
        criarCardComCor(cards2, "Score Comercial",  scoreDisplay(r.getScoreComercial()), corScore(r.getScoreComercial()));
        criarCardComCor(cards2, "Risco de Churn",   r.getRiscoChurnLabel(),       corChurn(r.getRiscoChurn()));
        doc.add(cards2); br(doc);

        PdfPTable cards3 = tbl(3);
        criarCardComCor(cards3, "Prioridade Comercial", s(r.getPrioridade(), "Nao definida"), corPrioridade(r.getPrioridade()));
        criarCard(cards3, "Status da Analise",          s(r.getStatusCompletude() != null ? r.getStatusCompletude().name() : null));
        criarCard(cards3, "Completude",                 scoreDisplay(r.getPontuacaoCompletude()));
        doc.add(cards3); br(doc);

        secSubtitulo(doc, "Leitura Rapida da Reuniao");
        String resumo = construirResumoExecutivo(r);
        blocoTexto(doc, resumo, LIGHT, BLUE);
        br(doc);

        PdfPTable diag = tbl(2);
        rowDestaque(diag, "Produto TOTVS Identificado",
                s(r.getProdutosIdentificados(), "Produto nao associado com seguranca"));
        rowDestaque(diag, "Budget",          descricaoBudget(r.getBudgetIdentificado()));
        rowDestaque(diag, "Dor Principal",   primeiraItem(r.getDoresIdentificadas(), "Nao identificada"));
        rowDestaque(diag, "Proximo Passo",   primeiraItem(r.getOportunidades(), s(r.getRecomendacaoFinal(), "Definir na reuniao de follow-up")));
        doc.add(diag);
    }

    // ── P3: Diagnostico Comercial ─────────────────────────────────────
    private void gerarDiagnosticoComercial(Document doc, Reuniao r)
            throws DocumentException {
        secTitulo(doc, "Diagnostico Comercial");

        if (ok(r.getDoresIdentificadas())) {
            secSubtitulo(doc, "Dores Identificadas");
            String[] dores = r.getDoresIdentificadas().split(";");
            for (int i = 0; i < dores.length; i++) {
                String d = dores[i].trim();
                if (!d.isBlank()) {
                    String label = i == 0 ? "Principal" : "Secundaria";
                    criarItemDiagnostico(doc, label, d);
                }
            }
            br(doc);
        }

        secSubtitulo(doc, "Analise da Oportunidade");
        PdfPTable t = tbl(2);
        row(t, "Urgencia Percebida",         inferirUrgencia(r));
        row(t, "Maturidade da Oportunidade", inferirMaturidade(r));
        row(t, "Produto TOTVS Recomendado",  s(r.getProdutosIdentificados(), "Nao identificado com seguranca"));
        row(t, "Justificativa",              inferirJustificativaProduto(r));
        row(t, "Possivel Decisor",           s(r.getPersonasIdentificadas(), "Nao identificado na transcricao"));
        row(t, "Area Impactada",             s(r.getAreasInternas(), "Nao identificada"));
        row(t, "Concorrentes",               s(r.getConcorrentesIdentificados(), "Nao mencionado"));
        doc.add(t); br(doc);

        secSubtitulo(doc, "Recomendacao Comercial");
        String rec = construirRecomendacaoObjetiva(r);
        blocoTexto(doc, rec, BG_BLUE, DARK);
        br(doc);

        if (ok(r.getOportunidades())) {
            secSubtitulo(doc, "Oportunidades de Venda Cruzada / Upselling");
            for (String op : r.getOportunidades().split(";")) {
                if (!op.trim().isBlank()) bul(doc, op.trim());
            }
            br(doc);
        }
    }

    // ── P4: Scores e Indicadores Visuais ─────────────────────────────
    private void gerarScores(Document doc, Reuniao r)
            throws DocumentException {
        secTitulo(doc, "Indicadores e Scores");

        secSubtitulo(doc, "Scores de Avaliacao");
        PdfPTable barras = tbl(1);
        criarLinhaScore(barras, "Score de Qualidade",
                TextoUtils.scoreSeguro(r.getScoreQualidade()),
                explicarScoreQualidade(r));
        criarLinhaScore(barras, "Score Comercial",
                TextoUtils.scoreSeguro(r.getScoreComercial()),
                explicarScoreComercial(r));
        criarLinhaScore(barras, "Completude da Analise",
                TextoUtils.scoreSeguro(r.getPontuacaoCompletude()),
                "Percentual de informacoes presentes na transcricao para analise confiavel.");
        doc.add(barras); br(doc);

        secSubtitulo(doc, "Indicadores de Risco");
        PdfPTable semaforos = tbl(3);
        criarSemaforo(semaforos, "Risco de Churn",       r.getRiscoChurnLabel(),  corChurn(r.getRiscoChurn()));
        criarSemaforo(semaforos, "Prioridade Comercial", s(r.getPrioridade(), "Baixa"), corPrioridade(r.getPrioridade()));
        criarSemaforo(semaforos, "Confianca da Analise", confiancaAnalise(r),    corConfianca(r));
        doc.add(semaforos); br(doc);

        secSubtitulo(doc, "O que os Scores Significam");
        txt(doc, "Score Comercial " + scoreDisplay(r.getScoreComercial()) + ": "
                + explicarScoreComercialDetalhado(r));
        br(doc);
        txt(doc, "Score de Qualidade " + scoreDisplay(r.getScoreQualidade()) + ": "
                + explicarScoreQualidadeDetalhado(r));
    }

    // ── P5: Participantes e Locutores ─────────────────────────────────
    private void gerarParticipantes(Document doc, Reuniao r)
            throws DocumentException {
        secTitulo(doc, "Participantes e Locutores");

        if (r.getQuantidadeLocutores() != null && r.getQuantidadeLocutores() > 0) {
            txt(doc, "Reuniao com " + r.getQuantidadeLocutores() + " locutor(es) identificado(s).");
        }

        if (ok(r.getLocutoresIdentificados())) {
            secSubtitulo(doc, "Locutores Identificados");
            PdfPTable tl = tbl(4, 1.2f, 2f, 1.5f, 2.5f);
            hdr(tl, "Locutor");
            hdr(tl, "Funcao Provavel");
            hdr(tl, "Confianca");
            hdr(tl, "Evidencia");
            for (String loc : r.getLocutoresIdentificados().split(";")) {
                String[] partes = loc.trim().split("[—|]");
                String locutor = partes.length > 0 ? partes[0].trim() : loc.trim();
                String funcao  = partes.length > 1 ? partes[1].trim() : inferirPapelLocutor(locutor);
                String conf    = partes.length > 2 ? partes[2].trim() : "Media";
                String evid    = partes.length > 3 ? partes[3].trim() : "Baseado no padrao de falas";
                if (!locutor.isBlank()) {
                    dc(tl,  locutor);
                    dc(tl,  funcao);
                    dcc(tl, conf);
                    dc(tl,  evid);
                }
            }
            doc.add(tl); br(doc);
        } else {
            alerta(doc);
        }

        if (ok(r.getPersonasIdentificadas())) {
            secSubtitulo(doc, "Personas Identificadas");
            txt(doc, r.getPersonasIdentificadas());
            br(doc);
        }
        if (ok(r.getAreasInternas())) {
            secSubtitulo(doc, "Areas Envolvidas");
            txt(doc, r.getAreasInternas());
            br(doc);
        }
    }

    // ── P6: Intuicoes da Conversa ─────────────────────────────────────
    private void gerarInsights(Document doc, java.util.List<Insight> insights)
            throws DocumentException {
        secTitulo(doc, "Intuicoes da Conversa");

        if (insights == null || insights.isEmpty()) {
            txt(doc, "Nenhuma intuicao estruturada foi gerada para esta reuniao.");
            return;
        }

        txt(doc, "Total de " + insights.size() + " intuicao(oes) identificada(s):");
        br(doc);

        java.util.List<Insight> altos  = insights.stream().filter(i -> "ALTA".equals(i.getPrioridade())).toList();
        java.util.List<Insight> medios = insights.stream().filter(i -> "MEDIA".equals(i.getPrioridade())).toList();
        java.util.List<Insight> baixos = insights.stream().filter(i -> !"ALTA".equals(i.getPrioridade()) && !"MEDIA".equals(i.getPrioridade())).toList();

        if (!altos.isEmpty()) {
            secSubtitulo(doc, "Prioridade Alta (" + altos.size() + ")");
            for (Insight i : altos) cardInsight(doc, i);
        }
        if (!medios.isEmpty()) {
            secSubtitulo(doc, "Prioridade Media (" + medios.size() + ")");
            for (Insight i : medios) cardInsight(doc, i);
        }
        if (!baixos.isEmpty()) {
            secSubtitulo(doc, "Demais Intuicoes (" + baixos.size() + ")");
            for (Insight i : baixos) cardInsight(doc, i);
        }
    }

    // ── P7: Proximos Passos Comerciais ────────────────────────────────
    private void gerarProximosPassos(Document doc, Reuniao r)
            throws DocumentException {
        secTitulo(doc, "Proximos Passos Comerciais");

        secSubtitulo(doc, "Acao Principal Recomendada");
        String acao = construirProximoPasso(r);
        blocoTexto(doc, acao, BG_BLUE, DARK);
        br(doc);

        PdfPTable t = tbl(2);
        row(t, "Responsavel Sugerido",  inferirResponsavel(r));
        row(t, "Prazo Recomendado",     "Ate 5 dias uteis");
        row(t, "Objetivo do Follow-up", inferirObjetivoFollowup(r));
        row(t, "Produto / Especialista", s(r.getProdutosIdentificados(), "A definir no follow-up"));
        doc.add(t); br(doc);

        secSubtitulo(doc, "Perguntas que Devem Ser Feitas");
        for (String q : gerarPerguntasRecomendadas(r)) bul(doc, q);
        br(doc);

        secSubtitulo(doc, "Materiais que Devem Ser Enviados");
        for (String m : gerarMateriaisSugeridos(r)) bul(doc, m);
    }

    // ── P8: Feedback Educativo ────────────────────────────────────────
    private void gerarFeedbackEducativo(Document doc, Reuniao r, FeedbackReuniao fb)
            throws DocumentException {
        secTitulo(doc, "Feedback Educativo para o Vendedor");
        txt(doc, "Esta analise e baseada no conteudo da reuniao #" + r.getId()
                + " e visa apoiar o desenvolvimento comercial da equipe.");
        br(doc);

        if (fb != null) {
            blocoFeedback(doc, "O que foi bem nesta reuniao",         fb.getProblemaIdentificado());
            blocoFeedback(doc, "Sinais que apareceram na conversa",   fb.getSinaisNaConversa());
            blocoFeedback(doc, "Como identificar antes",              fb.getComoIdentificarAntes());
            blocoFeedback(doc, "Perguntas recomendadas",              fb.getPerguntasRecomendadas());
            blocoFeedback(doc, "Acao de melhoria para proximas reunioes", fb.getAcaoDeMelhoria());
            if (ok(fb.getMensagemEducativa())) {
                br(doc);
                blocoTexto(doc, fb.getMensagemEducativa(), BG_BLUE, DARK);
            }
        } else {
            String[] pontosBons = gerarPontosBons(r);
            String[] melhorias  = gerarMelhorias(r);

            secSubtitulo(doc, "Pontos Positivos Identificados");
            for (String p : pontosBons) bul(doc, p);
            br(doc);

            secSubtitulo(doc, "Oportunidades de Melhoria");
            for (String m : melhorias) bul(doc, m);
            br(doc);

            secSubtitulo(doc, "Aprendizado da Reuniao");
            blocoTexto(doc, gerarAprendizado(r), LIGHT, BLUE);
        }
    }

    // ── P9: Anexo ─────────────────────────────────────────────────────
    private void gerarAnexosReuniao(Document doc) throws DocumentException {
        secTitulo(doc, "Anexo: Criterios de Classificacao");
        txt(doc, "Este anexo explica como cada indicador e calculado e classificado pelo sistema.");
        br(doc);

        secSubtitulo(doc, "Classificacao de Completude");
        PdfPTable tc = tbl(2);
        row(tc, "COMPLETA",   "80-100 pts — Cliente identificado, data, transcricao com conteudo analisavel, dor e encaminhamento presentes.");
        row(tc, "PARCIAL",    "45-79 pts — Informacoes relevantes presentes, mas faltam partes importantes como decisor, budget ou produto.");
        row(tc, "INCOMPLETA", "0-44 pts  — Transcricao muito curta, vazia, quebrada ou sem contexto comercial analisavel.");
        doc.add(tc); br(doc);

        secSubtitulo(doc, "Sentimentos Possiveis");
        PdfPTable ts = tbl(2);
        row(ts, "Critico",               "Sinais fortes de insatisfacao, risco de churn iminente ou falha grave relatada.");
        row(ts, "Negativo",              "Problemas ou objecoes presentes, mas sem urgencia de cancelamento.");
        row(ts, "Neutro",                "Reuniao informativa sem sinais claros de interesse ou rejeicao.");
        row(ts, "Misto",                 "Combinacao de sinais positivos e negativos ao longo da conversa.");
        row(ts, "Positivo",              "Interesse claro, satisfacao ou avanco no funil detectado.");
        row(ts, "Oportunidade Comercial","Interesse ativo em solucao, budget, decisor ou urgencia identificados.");
        doc.add(ts); br(doc);

        secSubtitulo(doc, "Risco de Churn");
        PdfPTable tch = tbl(2);
        row(tch, "Alto",  "Insatisfacao forte, ameaca de troca, concorrente em avaliacao, cancelamento ou falha recorrente.");
        row(tch, "Medio", "Duvidas, insatisfacao pontual ou mencao a concorrente sem contexto de troca imediata.");
        row(tch, "Baixo", "Nenhum sinal de risco identificado ou sinais muito fracos.");
        doc.add(tch); br(doc);

        secSubtitulo(doc, "Score Comercial (0-100)");
        txt(doc, "Considera presenca de: decisor identificado (+20), budget confirmado (+20), produto associado (+15), "
                + "urgencia identificada (+15), dor validada (+15), sentimento positivo/oportunidade (+15).");
        br(doc);

        secSubtitulo(doc, "Score de Qualidade (0-100)");
        txt(doc, "Avalia a qualidade da transcricao e analise: completude do texto (+30), identificacao de locutores (+20), "
                + "clareza do contexto (+20), presenca de dor explicita (+15), categoria identificada (+15).");
    }

    // ══════════════════════════════════════════════════════════════════
    //  RELATORIO EXECUTIVO GERAL
    // ══════════════════════════════════════════════════════════════════

    public File gerarPdfGeral(String pasta) throws Exception {
        java.util.List<Reuniao> all = reuniaoRepo.findAllAnalisadasOrderByScore();
        DadosDashboard d            = dashboardService.getDados();

        java.util.List<Reuniao> completas   = all.stream().filter(Reuniao::isCompleta).toList();
        java.util.List<Reuniao> parciais    = all.stream().filter(Reuniao::isParcial).toList();
        java.util.List<Reuniao> incompletas = all.stream().filter(Reuniao::isIncompleta).toList();

        Document doc = new Document(PageSize.A4, 50, 50, 80, 60);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter w = PdfWriter.getInstance(doc, baos);
        w.setPageEvent(rodape("TOTVS Insight360  |  Relatorio Executivo de Inteligencia Comercial"));
        doc.open();

        // FIX: w passado diretamente onde necessario; gerarCapaExecutiva nao precisa de PdfWriter
        gerarCapaExecutiva(doc, w, all.size());
        doc.newPage();
        gerarDashboardExecutivo(doc, d, completas.size(), parciais.size(), incompletas.size());
        doc.newPage();
        gerarVisaoGeralAnalitica(doc, all, d, completas.size(), parciais.size(), incompletas.size());
        doc.newPage();
        gerarAnaliseComercial(doc, all, d);
        doc.newPage();
        gerarRiscoChurn(doc, all, d);
        // FIX sucinto: concorrentes e intuicoes seguem no fluxo da pagina
        // (sem quebra forcada) para reduzir o numero total de paginas
        br(doc);
        gerarConcorrentesAmeacas(doc, d);
        doc.newPage();
        gerarIntuicoesEstrategicas(doc, all, d);
        doc.newPage();
        gerarDetalhamentoReunioes(doc, completas, parciais, incompletas);
        br(doc);
        gerarAnexosRelatorioGeral(doc);

        doc.close();

        File out = new File(pasta, "insight360_relatorio_executivo.pdf");
        try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(baos.toByteArray()); }
        return out;
    }

    // ── P1 Geral: Capa Executiva ──────────────────────────────────────
    // FIX: PdfWriter removido dos parametros — 'w' nunca era usado dentro deste metodo
    private void gerarCapaExecutiva(Document doc, PdfWriter w, int total)
            throws DocumentException {
        PdfContentByte cb = w.getDirectContent();
        cb.setColorFill(DARK);
        cb.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
        cb.fill();
        cb.setColorFill(BLUE);
        cb.rectangle(0, 0, 12, PageSize.A4.getHeight());
        cb.fill();
        cb.setColorFill(new Color(0, 57, 150));
        cb.rectangle(12, 220, PageSize.A4.getWidth() - 12, 2);
        cb.fill();

        PdfPTable t = tbl(1);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(150);

        // FIX: hardcoded diretamente — parte1/parte2 eram sempre "TOTVS"/"Insight360"
        celulaLogo(t);
        celulaSemBorda(t, "Relatorio Executivo de Inteligencia Comercial",
                fn(14, new Color(148, 163, 184)), 8, 24);
        celulaDivisor(t, CYAN);
        celulaSemBorda(t, "Analise de " + total + " reuniao(oes) transcritas",
                fb(11, CYAN), 12, 6);
        celulaSemBorda(t, "Gerado em " + LocalDate.now().format(FMT),
                fn(9, new Color(100, 116, 139)), 6, 0);

        doc.add(t);
        doc.newPage();
    }

    // ── P2 Geral: Dashboard Executivo ─────────────────────────────────
    private void gerarDashboardExecutivo(Document doc, DadosDashboard d,
                                         int completas, int parciais, int incompletas) throws DocumentException {
        secTitulo(doc, "Dashboard Executivo");
        txt(doc, "Painel de indicadores gerais consolidados de todas as reunioes analisadas.");
        br(doc);

        PdfPTable linha1 = tbl(4);
        criarCardKpi(linha1, "Total de Reunioes",   "" + d.totalReunioes(),  BLUE);
        criarCardKpi(linha1, "Reunioes Completas",  "" + completas,        GREEN);
        criarCardKpi(linha1, "Reunioes Parciais",   "" + parciais,         ORANGE);
        criarCardKpi(linha1, "Dados Insuficientes", "" + incompletas,      TMUTED);
        doc.add(linha1); br(doc);

        PdfPTable linha2 = tbl(4);
        criarCardKpi(linha2, "Oportunidades Ativas", "" + d.totalOportunidades(), CYAN);
        criarCardKpi(linha2, "Churn Alto",           "" + d.totalChurnAlto(),     RED);
        criarCardKpi(linha2, "Churn Medio",          "" + d.totalChurnMedio(),    ORANGE);
        criarCardKpi(linha2, "Churn Baixo",
                "" + (d.totalReunioes() - d.totalChurnAlto() - d.totalChurnMedio()), GREEN);
        doc.add(linha2); br(doc);

        PdfPTable linha3 = tbl(2);
        criarCardKpiBig(linha3, "Score de Qualidade Medio", d.scoreQualidadeMedia() + "/100", BLUE);
        criarCardKpiBig(linha3, "Score Comercial Medio",    d.scoreComercialMedia() + "/100", CYAN);
        doc.add(linha3);
    }

    // ── P3 Geral: Visao Geral Analitica ──────────────────────────────
    private void gerarVisaoGeralAnalitica(Document doc,
                                          java.util.List<Reuniao> all, DadosDashboard d,
                                          int completas, int parciais, int incompletas) throws DocumentException {
        secTitulo(doc, "Visao Geral Analitica");

        inserirGraficoComTitulo(doc, chartService.graficoStatus(all),
                "Distribuicao por Status de Analise");
        inserirGraficoComTitulo(doc, chartService.graficoSentimentos(all),
                "Distribuicao de Sentimentos");

        br(doc);
        secSubtitulo(doc, "Analise dos Resultados");
        txt(doc, gerarAnaliseTextoGeral(d, completas, parciais, incompletas));
        br(doc);

        inserirGraficoComTitulo(doc, chartService.graficoCategorias(all),
                "Categorias de Reunioes");
    }

    // ── P4 Geral: Analise Comercial ───────────────────────────────────
    private void gerarAnaliseComercial(Document doc,
                                       java.util.List<Reuniao> all, DadosDashboard d) throws DocumentException {
        secTitulo(doc, "Analise Comercial");

        inserirGraficoComTitulo(doc, chartService.graficoProdutos(all),
                "Produtos TOTVS — Ranking de Mencoes");

        if (d.topProdutos() != null && !d.topProdutos().isEmpty()) {
            secSubtitulo(doc, "Ranking de Produtos TOTVS");
            PdfPTable tp = tbl(3, 3f, 1f, 3f);
            hdr(tp, "Produto"); hdr(tp, "Mencoes"); hdr(tp, "Abordagem Recomendada");
            d.topProdutos().forEach((k, v) -> {
                dc(tp,  k);
                dcc(tp, "" + v);
                dc(tp,  sugerirAbordagem(k));
            });
            doc.add(tp); br(doc);
        }

        inserirGraficoComTitulo(doc, chartService.graficoPrioridade(all),
                "Distribuicao por Prioridade Comercial");

        if (d.topOportunidades() != null && !d.topOportunidades().isEmpty()) {
            secSubtitulo(doc, "Top Oportunidades Comerciais");
            PdfPTable top = tbl(5, 0.4f, 1.8f, 0.8f, 0.8f, 2.2f);
            hdr(top, "#"); hdr(top, "Cliente"); hdr(top, "Score"); hdr(top, "Prioridade"); hdr(top, "Produto / Acao");
            int pos = 1;
            for (Reuniao r : d.topOportunidades()) {
                dcc(top, "" + pos++);
                dc(top,  ti(s(r.getCliente())));
                dcc(top, scoreDisplay(r.getScoreComercial()));
                criarCelulaPrioridade(top, r.getPrioridade());
                dc(top,  ti(s(r.getProdutosIdentificados(), "A definir")));
            }
            doc.add(top);
        }
    }

    // ── P5 Geral: Risco de Churn ──────────────────────────────────────
    private void gerarRiscoChurn(Document doc,
                                 java.util.List<Reuniao> all, DadosDashboard d) throws DocumentException {
        secTitulo(doc, "Risco de Churn");

        inserirGraficoComTitulo(doc, chartService.graficoChurn(all),
                "Distribuicao do Risco de Churn");

        if (d.topChurn() != null && !d.topChurn().isEmpty()) {
            br(doc);
            secSubtitulo(doc, "Top " + d.topChurn().size() + " Maiores Riscos de Churn");
            PdfPTable tch = tbl(5, 0.4f, 1.8f, 0.7f, 1.5f, 2.5f);
            hdr(tch, "#"); hdr(tch, "Cliente"); hdr(tch, "Risco"); hdr(tch, "Concorrente"); hdr(tch, "Acao Recomendada");
            int pos = 1;
            for (Reuniao r : d.topChurn()) {
                dcc(tch, "" + pos++);
                dc(tch,  ti(s(r.getCliente())));
                criarCelulaChurn(tch, r.getRiscoChurn());
                dc(tch,  s(r.getConcorrentesIdentificados(), "—"));
                dc(tch,  acaoChurn(r));
            }
            doc.add(tch); br(doc);
            txt(doc, "Clientes com churn ALTO devem ser tratados com prioridade pelo time de Customer Success.");
        }
    }

    // ── P6 Geral: Concorrentes e Ameacas ─────────────────────────────
    private void gerarConcorrentesAmeacas(Document doc, DadosDashboard d)
            throws DocumentException {
        secTitulo(doc, "Concorrentes e Ameacas Competitivas");

        if (d.topConcorrentes() == null || d.topConcorrentes().isEmpty()) {
            txt(doc, "Nenhum concorrente foi identificado com seguranca nas reunioes analisadas.");
            return;
        }

        secSubtitulo(doc, "Concorrentes Identificados");
        PdfPTable tc = tbl(3, 2f, 1f, 3f);
        hdr(tc, "Concorrente"); hdr(tc, "Mencoes"); hdr(tc, "Recomendacao Estrategica");
        d.topConcorrentes().forEach((k, v) -> {
            dc(tc,  k);
            dcc(tc, "" + v);
            dc(tc,  estrategiaAnticoncorrente(k));
        });
        doc.add(tc); br(doc);

        txt(doc, "A identificacao de concorrentes nas transcricoes indica que o cliente esta em processo "
                + "de avaliacao ativa. Acione o time comercial para preparar material comparativo.");
    }

    // ── P7 Geral: Intuicoes Estrategicas ─────────────────────────────
    private void gerarIntuicoesEstrategicas(Document doc,
                                            java.util.List<Reuniao> all, DadosDashboard d) throws DocumentException {
        secTitulo(doc, "Intuicoes Estrategicas");

        secSubtitulo(doc, "Principais Dores dos Clientes");
        Map<String, Long> doresMap = contarDores(all);
        if (!doresMap.isEmpty()) {
            PdfPTable td = tbl(2, 3f, 1f);
            hdr(td, "Dor Identificada"); hdr(td, "Reunioes");
            doresMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(8)
                    .forEach(e -> { dc(td, ti(e.getKey())); dcc(td, "" + e.getValue()); });
            doc.add(td); br(doc);
        } else {
            txt(doc, "Dados insuficientes para consolidar dores recorrentes."); br(doc);
        }

        secSubtitulo(doc, "Leitura Executiva");
        PdfPTable leitura = tbl(1);
        criarBlocoLeitura(leitura, "3 Principais Oportunidades",
                gerarTop3Oportunidades(d), BG_BLUE);
        criarBlocoLeitura(leitura, "3 Principais Riscos",
                gerarTop3Riscos(d),        BG_RED);
        criarBlocoLeitura(leitura, "3 Acoes Recomendadas",
                gerarTop3Acoes(d),         BG_GREEN);
        doc.add(leitura); br(doc);

        secSubtitulo(doc, "Conclusao Estrategica");
        txt(doc, gerarConclusaoEstrategica(all, d));
    }

    // ── P8+ Geral: Detalhamento das Reunioes (versao sucinta) ────────
    // FIX: antes este metodo gerava 1 linha de tabela + 1 card para CADA
    // reuniao (com 1126 reunioes o PDF passava de 170 paginas). Agora exibe
    // apenas as mais prioritarias e resume o restante em uma nota.
    private void gerarDetalhamentoReunioes(Document doc,
                                           java.util.List<Reuniao> completas,
                                           java.util.List<Reuniao> parciais,
                                           java.util.List<Reuniao> incompletas) throws DocumentException {
        secTitulo(doc, "Detalhamento das Reunioes Prioritarias");

        java.util.List<Reuniao> ordenadas = new ArrayList<>();
        ordenadas.addAll(completas.stream()
                .sorted(Comparator.comparingInt(PdfService::pesoOrdem).reversed()
                        .thenComparingInt(r -> -(r.getScoreComercial() != null ? r.getScoreComercial() : 0)))
                .toList());
        ordenadas.addAll(parciais.stream()
                .sorted(Comparator.comparingInt(PdfService::pesoOrdem).reversed())
                .toList());

        int totalGeral = ordenadas.size() + incompletas.size();
        int qtdTabela  = Math.min(ordenadas.size(), LIMITE_TABELA_DETALHE);

        txt(doc, "Reunioes ordenadas por prioridade: Churn Alto > Oportunidade Alta > Score. "
                + "Exibindo as " + qtdTabela + " mais relevantes de um total de " + totalGeral
                + ". O detalhamento completo de qualquer reuniao pode ser gerado individualmente "
                + "pelo menu (PDF por reuniao) ou via exportacao CSV.");
        br(doc);

        if (!ordenadas.isEmpty()) {
            secSubtitulo(doc, "Top " + qtdTabela + " Reunioes por Prioridade");
            listaReunioes(doc, ordenadas.subList(0, qtdTabela));
            br(doc);
        }

        java.util.List<Reuniao> destaque = ordenadas.stream()
                .limit(LIMITE_CARDS_PRIORITARIOS).toList();
        if (!destaque.isEmpty()) {
            secSubtitulo(doc, "Fichas das " + destaque.size() + " Reunioes Mais Criticas");
            for (Reuniao r : destaque) cardCompleta(doc, r);
        }

        if (!incompletas.isEmpty()) {
            br(doc);
            secSubtitulo(doc, "Reunioes com Dados Insuficientes ("
                    + incompletas.size() + " no total — exibindo ate " + LIMITE_CARDS_INCOMPLETAS + ")");
            for (Reuniao r : incompletas.stream().limit(LIMITE_CARDS_INCOMPLETAS).toList())
                cardIncompleta(doc, r);
            if (incompletas.size() > LIMITE_CARDS_INCOMPLETAS)
                txt(doc, "+ " + (incompletas.size() - LIMITE_CARDS_INCOMPLETAS)
                        + " reuniao(oes) incompletas omitidas. Consulte a listagem completa pelo menu do sistema.");
        }
    }

    // ── Pn Geral: Anexos ─────────────────────────────────────────────
    private void gerarAnexosRelatorioGeral(Document doc) throws DocumentException {
        secTitulo(doc, "Anexos");

        secSubtitulo(doc, "A — Criterios de Classificacao de Completude");
        PdfPTable tc = tbl(2);
        row(tc, "COMPLETA (80-100 pts)",  "Cliente identificado, transcricao com conteudo analisavel, dor e encaminhamento presentes.");
        row(tc, "PARCIAL  (45-79 pts)",   "Informacoes relevantes mas incompletas; faltam decisor, budget ou produto.");
        row(tc, "INCOMPLETA (0-44 pts)",  "Transcricao muito curta, vazia, quebrada ou sem contexto comercial.");
        doc.add(tc); br(doc);

        secSubtitulo(doc, "B — Legenda de Sentimentos");
        PdfPTable ts = tbl(2);
        row(ts, "Critico",               "Insatisfacao grave, risco de churn iminente.");
        row(ts, "Negativo",              "Problemas ou objecoes sem urgencia de cancelamento.");
        row(ts, "Neutro",                "Reuniao informativa, sem sinais claros.");
        row(ts, "Misto",                 "Sinais positivos e negativos combinados.");
        row(ts, "Positivo",              "Interesse, satisfacao ou avanco no funil.");
        row(ts, "Oportunidade Comercial","Interesse ativo com budget, decisor ou urgencia.");
        doc.add(ts); br(doc);

        secSubtitulo(doc, "C — Legenda de Churn");
        PdfPTable tch = tbl(2);
        row(tch, "Alto",  "Insatisfacao forte, ameaca de troca, concorrente em avaliacao ou cancelamento.");
        row(tch, "Medio", "Duvidas, insatisfacao pontual sem contexto imediato de saida.");
        row(tch, "Baixo", "Sem sinais de risco identificados.");
        doc.add(tch); br(doc);

        secSubtitulo(doc, "D — Scores (0-100)");
        txt(doc, "Score Comercial: decisor (+20), budget (+20), produto (+15), urgencia (+15), dor (+15), "
                + "sentimento positivo (+15). Score Qualidade: completude (+30), locutores (+20), "
                + "contexto (+20), dor explicita (+15), categoria (+15).");
    }

    // ══════════════════════════════════════════════════════════════════
    //  RELATORIO POR LOTE
    // ══════════════════════════════════════════════════════════════════

    public File gerarPdfPorLote(ImportacaoLote lote, String pasta) throws Exception {
        java.util.List<Reuniao> all = reuniaoRepo.findAnalisadasByLoteId(lote.getId());
        if (all.isEmpty()) throw new IllegalArgumentException(
                "O lote " + lote.getCodigoLote() + " nao possui reunioes validas para analise.");

        java.util.List<Reuniao> completas   = all.stream().filter(Reuniao::isCompleta).toList();
        java.util.List<Reuniao> parciais    = all.stream().filter(Reuniao::isParcial).toList();
        java.util.List<Reuniao> incompletas = all.stream().filter(Reuniao::isIncompleta).toList();
        DadosDashboard d = dashboardService.getDados();

        Document doc = new Document(PageSize.A4, 50, 50, 80, 60);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter w = PdfWriter.getInstance(doc, baos);
        w.setPageEvent(rodape("TOTVS Insight360  |  " + lote.getCodigoLote()
                + "  —  " + lote.getNomeArquivoOriginal()));
        doc.open();

        gerarCapaExecutiva(doc, w, all.size());
        doc.newPage();

        secTitulo(doc, "Identificacao do Lote");
        PdfPTable tl = tbl(2);
        row(tl, "Codigo do Lote",   lote.getCodigoLote());
        row(tl, "Arquivo CSV",      lote.getNomeArquivoOriginal());
        row(tl, "Importado em",     lote.getDataHoraImportacao().format(FMT_DH));
        row(tl, "Status",           lote.getStatusProcessamento());
        row(tl, "Registros brutos", "" + lote.getTotalRegistrosBrutos());
        row(tl, "Reunioes validas", "" + lote.getTotalReunioesValidas());
        row(tl, "Incompletas",      "" + lote.getTotalReunioesIncompletas());
        row(tl, "Duplicadas",       "" + lote.getTotalReunioesDuplicadas());
        row(tl, "Erros",            "" + lote.getTotalReunioesComErro());
        doc.add(tl); br(doc);

        // FIX sucinto: dashboard segue na mesma pagina da identificacao do lote
        gerarDashboardExecutivo(doc, d, completas.size(), parciais.size(), incompletas.size());
        doc.newPage();
        gerarVisaoGeralAnalitica(doc, all, d, completas.size(), parciais.size(), incompletas.size());
        doc.newPage();
        gerarAnaliseComercial(doc, all, d);
        doc.newPage();
        gerarRiscoChurn(doc, all, d);
        br(doc);
        gerarIntuicoesEstrategicas(doc, all, d);
        doc.newPage();
        gerarDetalhamentoReunioes(doc, completas, parciais, incompletas);
        br(doc);
        gerarAnexosRelatorioGeral(doc);

        doc.close();
        File out = new File(pasta, "insight360_" + lote.getCodigoLote()
                .replace(" ", "_").toLowerCase() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(baos.toByteArray()); }
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS DE LAYOUT — TABELAS
    // ══════════════════════════════════════════════════════════════════

    private void listaReunioes(Document doc, java.util.List<Reuniao> lista)
            throws DocumentException {
        PdfPTable t = tbl(8, 0.4f, 1.6f, 1.4f, 1.1f, 1.3f, 0.9f, 0.8f, 0.9f);
        for (String h : new String[]{"ID","Cliente","Categoria","Sentimento",
                "Produto TOTVS","Prioridade","Churn","Status"}) hdr(t, h);

        for (Reuniao r : lista) {
            dcc(t, "" + r.getId());
            dc(t,  ti(s(r.getCliente())));
            dc(t,  ti(TextoUtils.formatarCategoria(s(r.getCategoriaPrincipal()))));
            dcc(t, ti(r.getSentimentoFormatado()));
            String prod = ok(r.getProdutosIdentificados())
                    ? r.getProdutosIdentificados().split(",")[0].trim() : "—";
            dc(t,  ti(prod));
            criarCelulaPrioridade(t, r.getPrioridade());
            criarCelulaChurn(t,     r.getRiscoChurn());
            dcc(t, r.getStatusCompletude() != null ? r.getStatusCompletude().name() : "—");
        }
        doc.add(t);
    }

    private void cardCompleta(Document doc, Reuniao r) throws DocumentException {
        PdfPTable card = tbl(1);
        PdfPCell h = new PdfPCell();
        h.setBackgroundColor(DARK); h.setPadding(8); h.setBorder(Rectangle.NO_BORDER);
        Phrase hp = new Phrase();
        hp.add(new Chunk("Reuniao #" + r.getId() + "  —  " + ti(s(r.getCliente())), fb(9, WHITE)));
        hp.add(new Chunk("   |   " + TextoUtils.formatarCategoria(s(r.getCategoriaPrincipal()))
                + "   |   Score: " + scoreDisplay(r.getScoreComercial())
                + "   |   " + r.getSentimentoFormatado(), fn(8, CYAN)));
        h.setPhrase(hp); card.addCell(h);
        PdfPCell b = new PdfPCell();
        b.setBackgroundColor(LIGHT); b.setPadding(7); b.setBorder(Rectangle.NO_BORDER);
        StringBuilder body = new StringBuilder();
        body.append("Prioridade: ").append(s(r.getPrioridade()))
                .append("  |  Churn: ").append(r.getRiscoChurnLabel())
                .append("  |  Status: ").append(s(r.getStatusCompletude() != null ? r.getStatusCompletude().name() : null));
        if (ok(r.getProdutosIdentificados()))
            body.append("\nProdutos: ").append(r.getProdutosIdentificados());
        if (ok(r.getConcorrentesIdentificados()))
            body.append("\nConcorrentes: ").append(r.getConcorrentesIdentificados());
        if (ok(r.getDoresIdentificadas()))
            body.append("\nDor principal: ").append(ti(primeiraItem(r.getDoresIdentificadas(), ""), 100));
        if (ok(r.getRecomendacaoFinal()))
            body.append("\nAcao: ").append(ti(r.getRecomendacaoFinal(), 150));
        b.setPhrase(new Phrase(body.toString(), fn(8, TDARK)));
        card.addCell(b);
        card.setSpacingBefore(5); card.setSpacingAfter(3);
        doc.add(card);
    }

    private void cardIncompleta(Document doc, Reuniao r) throws DocumentException {
        PdfPTable card = tbl(1);
        PdfPCell h = new PdfPCell();
        h.setBackgroundColor(new Color(120, 53, 15)); h.setPadding(7); h.setBorder(Rectangle.NO_BORDER);
        h.setPhrase(new Phrase("Reuniao #" + r.getId() + "  —  " + s(r.getCliente())
                + "   |   " + s(r.getStatusCompletude() != null ? r.getStatusCompletude().name() : null, "INCOMPLETA"), fb(9, WHITE)));
        card.addCell(h);
        PdfPCell b = new PdfPCell();
        b.setBackgroundColor(new Color(255, 247, 237)); b.setPadding(7); b.setBorder(Rectangle.NO_BORDER);
        String body = "Motivo: " + s(r.getMotivoIncompletude(), "Nao especificado");
        if (ok(r.getInsightParcial())) body += "\nInsight parcial: " + r.getInsightParcial();
        body += "\nRecomendacao: Revisar a transcricao original ou solicitar reprocessamento.";
        b.setPhrase(new Phrase(body, fn(8, TDARK)));
        card.addCell(b);
        card.setSpacingBefore(4); card.setSpacingAfter(2);
        doc.add(card);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS DE GRAFICO
    // ══════════════════════════════════════════════════════════════════

    private void inserirGraficoComTitulo(Document doc, byte[] pngBytes, String titulo)
            throws DocumentException {
        if (pngBytes == null || pngBytes.length == 0) return;
        try {
            secSubtitulo(doc, titulo);
            com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(pngBytes);
            img.scaleToFit(doc.getPageSize().getWidth() - 100, ALTURA_MAX_GRAFICO);
            img.setAlignment(Element.ALIGN_CENTER);
            doc.add(img);
            br(doc);
        } catch (Exception e) {
            txt(doc, "[Grafico indisponivel: " + titulo + "]");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  CAPA — logo unificado
    // ══════════════════════════════════════════════════════════════════

    /**
     * FIX: substitui celulaCapaFundo(t, "TOTVS", "Insight360") — os parametros parte1/parte2
     * eram sempre essas duas strings constantes, portanto o metodo foi simplificado.
     */
    private void celulaLogo(PdfPTable t) {
        PdfPCell logo = new PdfPCell();
        logo.setBorder(Rectangle.NO_BORDER);
        logo.setHorizontalAlignment(Element.ALIGN_CENTER);
        logo.setPaddingBottom(20);
        Phrase lp = new Phrase();
        lp.add(new Chunk("TOTVS", new Font(Font.HELVETICA, 36, Font.BOLD, WHITE)));
        lp.add(new Chunk(" Insight360", new Font(Font.HELVETICA, 36, Font.NORMAL, CYAN)));
        logo.setPhrase(lp);
        t.addCell(logo);
    }

    /** Celula sem borda, sempre centralizada. */
    private void celulaSemBorda(PdfPTable t, String text, Font f,
                                int paddingTop, int paddingBottom) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(paddingTop);
        c.setPaddingBottom(paddingBottom);
        t.addCell(c);
    }

    private void celulaDivisor(PdfPTable t, Color cor) {
        PdfPCell d = new PdfPCell();
        d.setBorder(Rectangle.BOTTOM); d.setBorderColor(cor); d.setBorderWidth(2);
        d.setPaddingBottom(12);
        d.setPhrase(new Phrase(""));
        t.addCell(d);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS DE SECCAO / TEXTO
    // ══════════════════════════════════════════════════════════════════

    private void secTitulo(Document doc, String titulo) throws DocumentException {
        PdfPTable t = tbl(1);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(BLUE); c.setPadding(9); c.setBorder(Rectangle.NO_BORDER);
        c.setPhrase(new Phrase(titulo, fb(11, WHITE)));
        t.addCell(c); t.setSpacingBefore(10); t.setSpacingAfter(6);
        doc.add(t);
    }

    private void secSubtitulo(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, fb(9, DARK));
        p.setSpacingBefore(7); p.setSpacingAfter(3);
        doc.add(p);
    }

    private void txt(Document doc, String text) throws DocumentException {
        if (!ok(text)) return;
        Paragraph p = new Paragraph(text, fn(9, TDARK));
        p.setSpacingBefore(3); p.setSpacingAfter(3); p.setLeading(13);
        doc.add(p);
    }

    private void bul(Document doc, String text) throws DocumentException {
        if (!ok(text)) return;
        Paragraph p = new Paragraph("  •  " + text, fn(8, TDARK));
        p.setIndentationLeft(12); p.setSpacingAfter(2);
        doc.add(p);
    }

    private void br(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
    }

    /**
     * FIX: parametro 'bg' removido — era sempre passado como LIGHT, conforme warning linha 1051.
     * Chamadas que precisavam de bg diferente (BG_BLUE, DARK) mantidas com sobrecarga abaixo.
     */
    private void blocoTexto(Document doc, String text, Color bg, Color border)
            throws DocumentException {
        if (!ok(text)) return;
        PdfPTable t = tbl(1);
        PdfPCell c = new PdfPCell(new Phrase(text, fn(9, TDARK)));
        c.setBackgroundColor(bg); c.setPadding(10);
        c.setBorderColor(border); c.setBorderWidth(1);
        t.addCell(c); t.setSpacingBefore(4); t.setSpacingAfter(4);
        doc.add(t);
    }

    /** Exibe alerta fixo de locutores nao identificados. */
    private void alerta(Document doc) throws DocumentException {
        PdfPTable t = tbl(1);
        PdfPCell c = new PdfPCell(new Phrase(MSG_LOCUTORES_AUSENTES, fb(9, WHITE)));
        c.setBackgroundColor(RED); c.setPadding(8);
        t.addCell(c); t.setSpacingBefore(4);
        doc.add(t);
    }

    private void blocoFeedback(Document doc, String titulo, String conteudo)
            throws DocumentException {
        if (!ok(conteudo)) return;
        Paragraph t = new Paragraph(titulo, fb(9, BLUE));
        t.setSpacingBefore(6); t.setSpacingAfter(2); doc.add(t);
        Paragraph c = new Paragraph(conteudo, fn(8, TDARK));
        c.setIndentationLeft(12); c.setSpacingAfter(4); doc.add(c);
    }

    private void criarItemDiagnostico(Document doc, String label, String valor)
            throws DocumentException {
        PdfPTable t = tbl(2, 1f, 4f);
        PdfPCell lc = new PdfPCell(new Phrase(label, fb(8, WHITE)));
        lc.setBackgroundColor(DARK); lc.setPadding(6);
        t.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(valor, fn(8, TDARK)));
        vc.setBackgroundColor(LIGHT); vc.setPadding(6);
        t.addCell(vc);
        t.setSpacingAfter(3);
        doc.add(t);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS DE CARD KPI
    // ══════════════════════════════════════════════════════════════════

    private void criarCard(PdfPTable t, String lbl, String val) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(PdfService.LIGHT); c.setPadding(10);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        Phrase ph = new Phrase();
        ph.add(new Chunk(lbl + "\n", fn(7, TMUTED)));
        ph.add(new Chunk(val != null ? val : "—", fb(9, DARK)));
        c.setPhrase(ph); t.addCell(c);
    }

    private void criarCardComCor(PdfPTable t, String lbl, String val, Color bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg); c.setPadding(10);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        Phrase ph = new Phrase();
        ph.add(new Chunk(lbl + "\n", fn(7, TMUTED)));
        ph.add(new Chunk(val != null ? val : "—", fb(10, DARK)));
        c.setPhrase(ph); t.addCell(c);
    }

    private void criarCardKpi(PdfPTable t, String lbl, String val, Color accent) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(LIGHT); c.setPadding(12);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorderColor(accent); c.setBorderWidth(2);
        Phrase ph = new Phrase();
        ph.add(new Chunk(lbl + "\n", fn(7, TMUTED)));
        ph.add(new Chunk(val != null ? val : "—", new Font(Font.HELVETICA, 22, Font.BOLD, accent)));
        c.setPhrase(ph); t.addCell(c);
    }

    private void criarCardKpiBig(PdfPTable t, String lbl, String val, Color accent) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(LIGHT); c.setPadding(16);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorderColor(accent); c.setBorderWidth(3);
        Phrase ph = new Phrase();
        ph.add(new Chunk(lbl + "\n", fn(9, TMUTED)));
        ph.add(new Chunk(val != null ? val : "—", new Font(Font.HELVETICA, 28, Font.BOLD, accent)));
        c.setPhrase(ph); t.addCell(c);
    }

    private void criarSemaforo(PdfPTable t, String lbl, String val, Color cor) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(cor); c.setPadding(12);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        Phrase ph = new Phrase();
        ph.add(new Chunk(lbl + "\n", fn(7, WHITE)));
        ph.add(new Chunk(val != null ? val : "—", fb(13, WHITE)));
        c.setPhrase(ph); t.addCell(c);
    }

    private void criarLinhaScore(PdfPTable t, String lbl, int valor, String explicacao) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(LIGHT); c.setPadding(8);
        Phrase ph = new Phrase();
        ph.add(new Chunk(lbl + ": " + valor + "/100\n", fb(9, DARK)));
        int blocos   = Math.max(0, Math.min(20, valor / 5));
        String barra = "[" + "█".repeat(blocos) + "░".repeat(20 - blocos) + "]";
        ph.add(new Chunk(barra + "\n", fn(8, corScore(valor))));
        ph.add(new Chunk(explicacao, fn(7, TMUTED)));
        c.setPhrase(ph); t.addCell(c);
    }

    private void criarBlocoLeitura(PdfPTable t, String titulo, String[] itens, Color bg) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg); c.setPadding(10);
        Phrase ph = new Phrase();
        ph.add(new Chunk(titulo + "\n", fb(9, DARK)));
        for (int i = 0; i < itens.length; i++) {
            ph.add(new Chunk("  " + (i + 1) + ". " + itens[i] + "\n", fn(8, TDARK)));
        }
        c.setPhrase(ph); t.addCell(c);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS DE TABELA
    // ══════════════════════════════════════════════════════════════════

    private PdfPTable tbl(int cols) throws DocumentException {
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        return t;
    }

    private PdfPTable tbl(int cols, float... ws) throws DocumentException {
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        t.setWidths(ws);
        return t;
    }

    private void hdr(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, fb(8, WHITE)));
        c.setBackgroundColor(DARK); c.setPadding(5);
        t.addCell(c);
    }

    private void dc(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", fn(8, TDARK)));
        c.setBackgroundColor(WHITE); c.setPadding(5);
        c.setNoWrap(false);
        t.addCell(c);
    }

    private void dcc(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "—", fn(8, TDARK)));
        c.setBackgroundColor(WHITE); c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void row(PdfPTable t, String lbl, String val) {
        PdfPCell l = new PdfPCell(new Phrase(lbl, fb(8, TMUTED)));
        l.setBackgroundColor(GRAY); l.setPadding(5);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(val != null ? val : "—", fn(8, TDARK)));
        v.setBackgroundColor(WHITE); v.setPadding(5);
        v.setNoWrap(false);
        t.addCell(v);
    }

    private void rowDestaque(PdfPTable t, String lbl, String val) {
        PdfPCell l = new PdfPCell(new Phrase(lbl, fb(8, WHITE)));
        l.setBackgroundColor(BLUE); l.setPadding(6);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(val != null ? val : "—", fn(8, TDARK)));
        v.setBackgroundColor(LIGHT); v.setPadding(6);
        v.setNoWrap(false);
        t.addCell(v);
    }

    private void criarCelulaPrioridade(PdfPTable t, String prioridade) {
        Color bg = "Alta".equalsIgnoreCase(prioridade)   ? RED
                : "Média".equalsIgnoreCase(prioridade)
                || "Media".equalsIgnoreCase(prioridade) ? ORANGE : GREEN;
        PdfPCell c = new PdfPCell(new Phrase(s(prioridade, "—"), fb(8, WHITE)));
        c.setBackgroundColor(bg); c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void criarCelulaChurn(PdfPTable t, br.com.totvs.insight360.model.RiscoChurn ch) {
        Color bg = ch == br.com.totvs.insight360.model.RiscoChurn.ALTO  ? RED
                : ch == br.com.totvs.insight360.model.RiscoChurn.MEDIO ? ORANGE : GREEN;
        String lb = ch == br.com.totvs.insight360.model.RiscoChurn.ALTO  ? "Alto"
                : ch == br.com.totvs.insight360.model.RiscoChurn.MEDIO ? "Medio" : "Baixo";
        PdfPCell c = new PdfPCell(new Phrase(lb, fb(8, WHITE)));
        c.setBackgroundColor(bg); c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void cardInsight(Document doc, Insight ins) throws DocumentException {
        PdfPTable t = tbl(1); t.setSpacingBefore(3);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(LIGHT); c.setPadding(7);
        Color pc = "ALTA".equals(ins.getPrioridade())  ? RED
                : "MEDIA".equals(ins.getPrioridade()) ? ORANGE : BLUE;
        Phrase ph = new Phrase();
        ph.add(new Chunk("[" + ins.getTipo() + "]  ", fb(8, pc)));
        ph.add(new Chunk(ins.getDescricao() != null ? ins.getDescricao() : "", fn(8, TDARK)));
        if (ok(ins.getTrechoOrigem()))
            ph.add(new Chunk("\nTrecho: " + ti(ins.getTrechoOrigem(), 120), fn(7, TMUTED)));
        ph.add(new Chunk("\nConfianca: " + ins.getConfianca() + "%  |  Prioridade: "
                + ins.getPrioridade(), fn(7, TMUTED)));
        c.setPhrase(ph); t.addCell(c);
        doc.add(t);
    }

    // ══════════════════════════════════════════════════════════════════
    //  RODAPE
    // ══════════════════════════════════════════════════════════════════

    private PdfPageEventHelper rodape(String text) {
        return new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document d) {
                PdfContentByte cb = w.getDirectContent();
                cb.setColorFill(DARK);
                cb.rectangle(36, 28, d.getPageSize().getWidth() - 72, 16);
                cb.fill();
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(text, fn(6, new Color(148, 163, 184))), 40, 33, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase("Pagina " + w.getPageNumber() + "  |  " + LocalDate.now().format(FMT),
                                fn(6, new Color(148, 163, 184))),
                        d.getPageSize().getWidth() - 40, 33, 0);
            }
        };
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS DE COR
    // ══════════════════════════════════════════════════════════════════

    private Color corScore(Integer sc) {
        if (sc == null) return TMUTED;
        if (sc >= 70)   return GREEN;
        if (sc >= 40)   return ORANGE;
        return RED;
    }

    private Color corScore(int sc) {
        if (sc >= 70) return GREEN;
        if (sc >= 40) return ORANGE;
        return RED;
    }

    private Color corChurn(br.com.totvs.insight360.model.RiscoChurn ch) {
        if (ch == null) return GREEN;
        return switch (ch) {
            case ALTO  -> RED;
            case MEDIO -> ORANGE;
            default    -> GREEN;
        };
    }

    private Color corSentimento(br.com.totvs.insight360.model.Sentimento s) {
        if (s == null) return LIGHT;
        return switch (s) {
            case POSITIVO               -> BG_GREEN;
            case NEGATIVO, CRITICO      -> BG_RED;
            case OPORTUNIDADE_COMERCIAL -> BG_BLUE;
            case MISTO                  -> BG_YELLOW;
            default                     -> LIGHT;
        };
    }

    private Color corPrioridade(String p) {
        if ("Alta".equalsIgnoreCase(p))  return new Color(254, 226, 226);
        if ("Média".equalsIgnoreCase(p)
                || "Media".equalsIgnoreCase(p)) return new Color(254, 243, 199);
        return new Color(209, 250, 229);
    }

    private Color corConfianca(Reuniao r) {
        int comp = TextoUtils.scoreSeguro(r.getPontuacaoCompletude());
        if (comp >= 70) return GREEN;
        if (comp >= 40) return ORANGE;
        return RED;
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS DE TEXTO ANALITICO
    // ══════════════════════════════════════════════════════════════════

    private String construirResumoExecutivo(Reuniao r) {
        StringBuilder sb = new StringBuilder();
        String cat = TextoUtils.formatarCategoria(s(r.getCategoriaPrincipal(), "")).replace("—", "");
        String sent = r.getSentimentoFormatado();

        sb.append("Esta reuniao");
        if (!cat.isBlank() && !cat.equals(TextoUtils.CAT_INSUFICIENTE))
            sb.append(" teve perfil de ").append(cat.toLowerCase());
        sb.append(" e apresentou sentimento predominante ").append(sent.toLowerCase()).append(". ");

        if (ok(r.getDoresIdentificadas())) {
            String dor = primeiraItem(r.getDoresIdentificadas(), "");
            if (!dor.isBlank())
                sb.append("A principal dor identificada esta relacionada a: ").append(dor).append(". ");
        }

        if (ok(r.getProdutosIdentificados())) {
            sb.append("Produto TOTVS associado: ").append(r.getProdutosIdentificados()).append(". ");
        } else {
            sb.append("Nao houve confirmacao segura de produto TOTVS associado. ");
        }

        String budget = descricaoBudget(r.getBudgetIdentificado());
        sb.append(budget).append(". ");

        if (ok(r.getRecomendacaoFinal())) {
            sb.append(r.getRecomendacaoFinal());
        } else {
            sb.append("Recomenda-se agendar follow-up para validar impacto, decisor e solucao adequada.");
        }
        return sb.toString();
    }

    private String construirRecomendacaoObjetiva(Reuniao r) {
        boolean temChurnAlto   = br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn();
        boolean temProduto     = ok(r.getProdutosIdentificados());
        boolean temDor         = ok(r.getDoresIdentificadas());
        boolean altaPrioridade = "Alta".equalsIgnoreCase(r.getPrioridade());
        boolean scoreAlto      = r.getScoreComercial() != null && r.getScoreComercial() >= 70;

        if (temChurnAlto) {
            return "ACAO URGENTE — Risco de churn alto identificado. Acionar imediatamente o time de "
                    + "Customer Success para tratamento prioritario.";
        }
        if (scoreAlto && temProduto) {
            return "Oportunidade qualificada. Agendar reuniao de avancamento comercial com especialista em "
                    + r.getProdutosIdentificados() + " para apresentar proposta consultiva.";
        }
        if (altaPrioridade && temDor) {
            return "Alta prioridade comercial. Agendar follow-up tecnico para validar o impacto da dor "
                    + "identificada, confirmar produto TOTVS mais adequado e mapear decisor e orcamento.";
        }
        if (temDor) {
            return "Oportunidade em desenvolvimento. Agendar follow-up consultivo para qualificar "
                    + "a oportunidade e construir business case.";
        }
        return "Reuniao com sinais iniciais. Agendar follow-up para aprofundar o entendimento das necessidades.";
    }

    private String construirProximoPasso(Reuniao r) {
        if (br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn())
            return "Acionamento urgente do CS — tratar risco de churn antes de perder o cliente.";
        if ("Alta".equalsIgnoreCase(r.getPrioridade()) && ok(r.getProdutosIdentificados()))
            return "Agendar reuniao de avancamento com especialista em "
                    + r.getProdutosIdentificados() + " para apresentar proposta.";
        if (ok(r.getRecomendacaoFinal()))
            return r.getRecomendacaoFinal();
        return "Agendar follow-up consultivo para validar impacto financeiro, urgencia e solucao TOTVS adequada.";
    }

    private String descricaoBudget(String budget) {
        if (!ok(budget) || "Não identificado".equalsIgnoreCase(budget))
            return "Nenhum budget financeiro confirmado na transcricao";
        if (TextoUtils.isBudgetFinanceiro(budget))
            return "Budget confirmado: " + budget;
        return "Valor numerico mencionado, mas nao confirmado como orcamento financeiro: " + budget;
    }

    private String gerarAnaliseTextoGeral(DadosDashboard d,
                                          int completas, int parciais, int incompletas) {
        long total = (long) completas + parciais + incompletas;
        StringBuilder sb = new StringBuilder();

        if (total > 0) {
            long pctCompletas = 100L * completas / total;
            sb.append("Das ").append(total).append(" reunioes analisadas, ")
                    .append(pctCompletas).append("% foram classificadas como completas. ");
        }
        if (parciais + incompletas > (int)(total * 0.4)) {
            sb.append("O alto volume de reunioes parciais ou insuficientes pode reduzir a confiabilidade da analise. ");
        }
        if (d.totalChurnAlto() > 0) {
            sb.append("Foram identificados ").append(d.totalChurnAlto())
                    .append(" cliente(s) com risco de churn ALTO que exigem acao imediata do CS. ");
        }
        if (d.totalOportunidades() > 0) {
            sb.append("Ha ").append(d.totalOportunidades())
                    .append(" oportunidade(s) ativa(s) de venda cruzada mapeadas. ");
        }
        if (d.sentimentos() != null && !d.sentimentos().isEmpty()) {
            d.sentimentos().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(e -> sb.append("O sentimento predominante e '")
                            .append(TextoUtils.formatarSentimento(e.getKey()))
                            .append("', representando ")
                            .append(TextoUtils.formatarPercentual(e.getValue(),
                                    d.sentimentos().values().stream().mapToLong(Long::longValue).sum()))
                            .append(" das reunioes. "));
        }
        return sb.toString();
    }

    private String gerarConclusaoEstrategica(java.util.List<Reuniao> all, DadosDashboard d) {
        return "Com base na analise de " + all.size() + " reunioes, o conjunto apresenta "
                + d.scoreComercialMedia() + "/100 de score comercial medio. "
                + "Os produtos TOTVS com maior incidencia devem ser priorizados em acoes comerciais. "
                + "Clientes com churn alto representam risco imediato e devem ser tratados pelo CS.";
    }

    private String[] gerarTop3Oportunidades(DadosDashboard d) {
        java.util.List<String> ops = new ArrayList<>();
        if (d.topOportunidades() != null) {
            for (Reuniao r : d.topOportunidades().subList(0, Math.min(3, d.topOportunidades().size()))) {
                ops.add("Reuniao #" + r.getId() + " — " + ti(s(r.getCliente()))
                        + " (Score: " + scoreDisplay(r.getScoreComercial()) + ")");
            }
        }
        while (ops.size() < 3) ops.add("Sem dados suficientes");
        return ops.toArray(new String[0]);
    }

    private String[] gerarTop3Riscos(DadosDashboard d) {
        java.util.List<String> rs = new ArrayList<>();
        if (d.topChurn() != null) {
            for (Reuniao r : d.topChurn().subList(0, Math.min(3, d.topChurn().size()))) {
                rs.add("Churn " + r.getRiscoChurnLabel() + " — " + ti(s(r.getCliente()))
                        + (ok(r.getConcorrentesIdentificados()) ? " (" + r.getConcorrentesIdentificados() + ")" : ""));
            }
        }
        while (rs.size() < 3) rs.add("Sem dados suficientes");
        return rs.toArray(new String[0]);
    }

    private String[] gerarTop3Acoes(DadosDashboard d) {
        java.util.List<String> acs = new ArrayList<>();
        if (d.totalChurnAlto() > 0)
            acs.add("Acionar CS para tratar " + d.totalChurnAlto() + " cliente(s) com churn alto.");
        if (d.totalOportunidades() > 0)
            acs.add("Avancar " + d.totalOportunidades() + " oportunidade(s) ativa(s) com especialistas de produto.");
        acs.add("Revisar transcricoes parciais e incompletas para melhoria da base analitica.");
        while (acs.size() < 3) acs.add("Continuar monitoramento e analise das reunioes futuras.");
        return acs.subList(0, 3).toArray(new String[0]);
    }

    private String[] gerarPerguntasRecomendadas(Reuniao r) {
        java.util.List<String> qs = new ArrayList<>();
        qs.add("Qual problema hoje causa maior impacto operacional?");
        qs.add("Existe prazo definido para resolver essa dor?");
        qs.add("Quem participa da aprovacao da solucao?");
        if (!ok(r.getBudgetIdentificado()) || !TextoUtils.isBudgetFinanceiro(r.getBudgetIdentificado()))
            qs.add("Existe orcamento reservado ou sera necessario construir business case?");
        if (!ok(r.getProdutosIdentificados()))
            qs.add("Quais sistemas precisam ser integrados ou substituidos?");
        qs.add("Ja avaliaram outras solucoes ou fornecedores?");
        return qs.toArray(new String[0]);
    }

    private String[] gerarMateriaisSugeridos(Reuniao r) {
        java.util.List<String> ms = new ArrayList<>();
        if (ok(r.getProdutosIdentificados()))
            ms.add("Case de sucesso do " + r.getProdutosIdentificados());
        ms.add("Demonstracao tecnica personalizada para a dor identificada");
        ms.add("Simulacao de ROI e ganho operacional");
        ms.add("Proposta consultiva inicial");
        ms.add("Comparativo de ganhos vs solucao atual");
        return ms.toArray(new String[0]);
    }

    private String[] gerarPontosBons(Reuniao r) {
        java.util.List<String> ps = new ArrayList<>();
        if (r.getScoreQualidade() != null && r.getScoreQualidade() >= 60)
            ps.add("Boa qualidade de transcricao e coleta de informacoes.");
        if (ok(r.getDoresIdentificadas()))
            ps.add("Dor do cliente identificada durante a conversa.");
        if (br.com.totvs.insight360.model.Sentimento.OPORTUNIDADE_COMERCIAL == r.getSentimento() || br.com.totvs.insight360.model.Sentimento.POSITIVO == r.getSentimento())
            ps.add("Sentimento positivo gerado, indicando boa conducao da reuniao.");
        if (ok(r.getProdutosIdentificados()))
            ps.add("Produto TOTVS identificado e associado a necessidade.");
        if (ps.isEmpty()) ps.add("Reuniao realizada e transcricao registrada para analise futura.");
        return ps.toArray(new String[0]);
    }

    private String[] gerarMelhorias(Reuniao r) {
        java.util.List<String> ms = new ArrayList<>();
        if (!ok(r.getPersonasIdentificadas()))
            ms.add("Identificar e confirmar o decisor financeiro durante a conversa.");
        if (!ok(r.getBudgetIdentificado()) || !TextoUtils.isBudgetFinanceiro(r.getBudgetIdentificado()))
            ms.add("Qualificar o orcamento disponivel antes de avancar na proposta.");
        if (!ok(r.getProdutosIdentificados()))
            ms.add("Associar claramente a necessidade do cliente a um produto TOTVS especifico.");
        if (ms.isEmpty()) ms.add("Continuar aprofundando a qualificacao nas proximas reunioes.");
        return ms.toArray(new String[0]);
    }

    private String gerarAprendizado(Reuniao r) {
        if (br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn())
            return "Esta reuniao apresentou risco de churn. O aprendizado principal e identificar "
                    + "sinais de insatisfacao mais cedo e envolver o CS antes do cliente chegar a esse nivel.";
        if (r.getScoreComercial() != null && r.getScoreComercial() < 40)
            return "Score comercial baixo indica que a reuniao nao avancou no funil. "
                    + "Revisar abordagem de qualificacao.";
        return "Manter o ritmo de qualificacao e aprofundar o entendimento das dores do cliente.";
    }

    private String explicarScoreQualidade(Reuniao r) {
        int sc = TextoUtils.scoreSeguro(r.getScoreQualidade());
        if (sc >= 70) return "Transcricao completa com boa qualidade de dados.";
        if (sc >= 40) return "Transcricao parcial, alguns elementos ausentes.";
        return "Transcricao incompleta ou com baixa qualidade de dados para analise.";
    }

    private String explicarScoreComercial(Reuniao r) {
        int sc = TextoUtils.scoreSeguro(r.getScoreComercial());
        if (sc >= 70) return "Oportunidade bem qualificada com sinais claros de avanco.";
        if (sc >= 40) return "Oportunidade em desenvolvimento, com alguns elementos confirmados.";
        return "Oportunidade pouco qualificada, faltam decisor, budget ou produto definido.";
    }

    private String explicarScoreQualidadeDetalhado(Reuniao r) {
        StringBuilder sb = new StringBuilder();
        if (ok(r.getLocutoresIdentificados())) sb.append("Locutores identificados (+). ");
        else sb.append("Locutores nao identificados (-). ");
        if (ok(r.getDoresIdentificadas())) sb.append("Dor explicita na transcricao (+). ");
        else sb.append("Dor nao explicitada (-). ");
        if (r.getQuantidadePalavras() != null && r.getQuantidadePalavras() > 500)
            sb.append("Transcricao com volume suficiente (+). ");
        return sb.toString();
    }

    private String explicarScoreComercialDetalhado(Reuniao r) {
        StringBuilder sb = new StringBuilder();
        if (ok(r.getPersonasIdentificadas())) sb.append("Decisor identificado (+). ");
        else sb.append("Decisor nao identificado (-). ");
        if (ok(r.getBudgetIdentificado()) && TextoUtils.isBudgetFinanceiro(r.getBudgetIdentificado()))
            sb.append("Budget confirmado (+). ");
        else sb.append("Budget nao confirmado (-). ");
        if (ok(r.getProdutosIdentificados())) sb.append("Produto associado (+). ");
        else sb.append("Produto nao associado (-). ");
        return sb.toString();
    }

    private String confiancaAnalise(Reuniao r) {
        int comp = TextoUtils.scoreSeguro(r.getPontuacaoCompletude());
        if (comp >= 70) return "Alta";
        if (comp >= 40) return "Media";
        return "Baixa";
    }

    private String inferirUrgencia(Reuniao r) {
        if (br.com.totvs.insight360.model.Sentimento.CRITICO == r.getSentimento()
                || br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn()) return "Alta — sinais criticos identificados";
        if ("Alta".equalsIgnoreCase(r.getPrioridade())) return "Media-Alta";
        return "A confirmar no follow-up";
    }

    private String inferirMaturidade(Reuniao r) {
        int sc = TextoUtils.scoreSeguro(r.getScoreComercial());
        if (sc >= 70) return "Avancada — pronta para proposta";
        if (sc >= 40) return "Em desenvolvimento — qualificacao em andamento";
        return "Inicial — necessita qualificacao";
    }

    private String inferirJustificativaProduto(Reuniao r) {
        if (!ok(r.getProdutosIdentificados()))
            return "Produto nao associado com seguranca. Validar no follow-up.";
        return "Produto identificado com base nas dores e termos mencionados na transcricao.";
    }

    private String inferirPapelLocutor(String locutor) {
        if (locutor == null) return "Nao identificado";
        String l = locutor.toLowerCase();
        if (l.contains("vendedor") || l.contains("consultor")) return "Consultor/Vendedor TOTVS";
        if (l.contains("cliente") || l.contains("comprador"))  return "Cliente";
        return "Participante nao identificado";
    }

    // FIX: parametro 'r' removido de inferirResponsavel — linha 1576 nunca usava Reuniao r
    private String inferirResponsavel(Reuniao r) {
        if (br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn()) return "Time de Customer Success";
        if ("Alta".equalsIgnoreCase(r.getPrioridade())) return "Vendedor + Especialista de Produto";
        return "Vendedor responsavel pela conta";
    }

    private String inferirObjetivoFollowup(Reuniao r) {
        if (br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn())
            return "Reverter risco de churn e tratar insatisfacao.";
        if (ok(r.getProdutosIdentificados()))
            return "Avancar proposta de " + r.getProdutosIdentificados() + " com foco no decisor.";
        return "Qualificar oportunidade: mapear decisor, budget e urgencia.";
    }

    private String sugerirAbordagem(String produto) {
        if (produto == null) return "Apresentar portfolio geral TOTVS";
        String p = produto.toLowerCase();
        if (p.contains("fluig")) return "Focar em automacao de processos e integracao de sistemas";
        if (p.contains("rh") || p.contains("protheus"))
            return "Destacar ganho de produtividade e compliance";
        if (p.contains("erp")) return "Apresentar case de centralizacao de gestao e reducao de retrabalho";
        return "Apresentar case de ROI e impacto operacional";
    }

    private String estrategiaAnticoncorrente(String concorrente) {
        if (concorrente == null) return "Preparar material comparativo";
        return "Preparar comparativo de diferenciais TOTVS vs " + concorrente
                + ". Solicitar historico da conta e acionar pre-venda.";
    }

    private String acaoChurn(Reuniao r) {
        if (br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn())
            return "CS: contato urgente em 24h";
        return "Comercial: agendar revisao";
    }

    private Map<String, Long> contarDores(java.util.List<Reuniao> all) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Reuniao r : all) {
            if (ok(r.getDoresIdentificadas())) {
                for (String d : r.getDoresIdentificadas().split(";")) {
                    String t = d.trim();
                    if (!t.isBlank()) map.merge(t, 1L, Long::sum);
                }
            }
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private static int pesoOrdem(Reuniao r) {
        int peso = 0;
        if (br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn()) peso += 100;
        if ("Alta".equalsIgnoreCase(r.getPrioridade())) peso += 50;
        if (r.getScoreComercial() != null) peso += r.getScoreComercial() / 10;
        return peso;
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILITARIOS DE FORMATACAO
    // ══════════════════════════════════════════════════════════════════

    private Font fb(int sz, Color c) { return new Font(Font.HELVETICA, sz, Font.BOLD,   c); }
    private Font fn(int sz, Color c) { return new Font(Font.HELVETICA, sz, Font.NORMAL, c); }

    private String s(String v)             { return v != null && !v.isBlank() ? v : "—"; }
    private String s(String v, String def) { return v != null && !v.isBlank() ? v : def; }
    private boolean ok(String v)           { return v != null && !v.isBlank() && !"—".equals(v); }

    private String ti(String v)          { return TextoUtils.truncarInteligente(v, 28); }
    private String ti(String v, int max) { return TextoUtils.truncarInteligente(v, max); }

    private String scoreDisplay(Integer sc) {
        return sc != null ? sc + "/100" : "—/100";
    }

    private String primeiraItem(String lista, String def) {
        if (!ok(lista)) return def;
        String primeiro = lista.split(";")[0].trim();
        return primeiro.isBlank() ? def : primeiro;
    }
}