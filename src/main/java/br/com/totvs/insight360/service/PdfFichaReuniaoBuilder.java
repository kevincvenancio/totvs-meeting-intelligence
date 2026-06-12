package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.FeedbackReuniao;
import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

/**
 * Builder responsável pela geração da ficha individual de cada reunião no relatório PDF.
 * <p>
 * Extraído de {@link PdfService} para respeitar o Princípio da Responsabilidade Única:
 * a ficha de reunião é uma seção autossuficiente dentro do relatório executivo.
 * <p>
 * Este builder é injetado em {@link PdfService}, que orquestra a montagem final
 * do documento chamando os métodos públicos aqui definidos.
 */
@Service
@RequiredArgsConstructor
public class PdfFichaReuniaoBuilder {

    private final FeedbackReuniaoService feedbackReuniaoService;

    // Paleta TOTVS (subconjunto utilizado na ficha)
    private static final Color DARK  = new Color(0,   20,  60);
    private static final Color BLUE  = new Color(0,   87,  255);
    private static final Color LIGHT = new Color(240, 245, 255);


    /**
     * Gera e adiciona ao documento todos os blocos da ficha de uma reunião:
     * resumo executivo, diagnóstico comercial, scores, insights e feedback.
     *
     * @param doc      documento iText aberto (já posicionado na página correta)
     * @param insights lista de insights vinculados à reunião
     * @param r        reunião a ser detalhada
     */
    // Corrigido: parâmetro 'w' nunca usado removido; @SuppressWarnings para falso positivo de método público
    @SuppressWarnings("unused")
    public void gerarFicha(Document doc, Reuniao r, List<Insight> insights)
            throws DocumentException {
        adicionarCabecalhoFicha(doc, r);
        adicionarResumoFicha(doc, r);
        adicionarInsightsFicha(doc, insights);
        Optional<FeedbackReuniao> fb = feedbackReuniaoService.buscarPorReuniao(r.getId());
        adicionarFeedbackFicha(doc, fb.orElse(null));
    }

    // ── Seções da ficha ───────────────────────────────────────────────

    private void adicionarCabecalhoFicha(Document doc, Reuniao r) throws DocumentException {
        com.lowagie.text.Paragraph titulo = new com.lowagie.text.Paragraph(
                "Reunião #" + r.getId() + " — " + safe(r.getCliente()),
                fb(12, DARK));
        titulo.setSpacingBefore(10);
        titulo.setSpacingAfter(6);
        doc.add(titulo);

        PdfPTable info = tbl(3);
        celula(info, "Status: " + safe(r.getStatusCompletude() != null ? r.getStatusCompletude().name() : null), LIGHT);
        celula(info, "Sentimento: " + r.getSentimentoFormatado(), corSentimento(r));
        celula(info, "Churn: " + r.getRiscoChurnLabel(), corChurn(r));
        doc.add(info);
    }

    private void adicionarResumoFicha(Document doc, Reuniao r) throws DocumentException {
        if (ok(r.getResumoReuniao())) {
            com.lowagie.text.Paragraph sub = new com.lowagie.text.Paragraph("Resumo", fb(10, BLUE));
            sub.setSpacingBefore(8);
            sub.setSpacingAfter(4);
            doc.add(sub);

            com.lowagie.text.Paragraph resumo = new com.lowagie.text.Paragraph(r.getResumoReuniao(), fn(9, new Color(15, 23, 42)));
            resumo.setIndentationLeft(10);
            resumo.setSpacingAfter(6);
            doc.add(resumo);
        }

        if (ok(r.getDoresIdentificadas())) {
            com.lowagie.text.Paragraph sub = new com.lowagie.text.Paragraph("Dores Identificadas", fb(10, BLUE));
            sub.setSpacingBefore(4);
            sub.setSpacingAfter(2);
            doc.add(sub);

            PdfPTable t = tbl(1);
            PdfPCell c = new PdfPCell(new Phrase(r.getDoresIdentificadas(), fn(9, new Color(15, 23, 42))));
            c.setBackgroundColor(LIGHT);
            c.setPadding(8);
            t.addCell(c);
            doc.add(t);
        }
    }

    private void adicionarInsightsFicha(Document doc, List<Insight> insights) throws DocumentException {
        if (insights == null || insights.isEmpty()) return;

        com.lowagie.text.Paragraph sub = new com.lowagie.text.Paragraph("Insights Extraídos", fb(10, BLUE));
        sub.setSpacingBefore(8);
        sub.setSpacingAfter(4);
        doc.add(sub);

        PdfPTable t = tbl(2);
        for (Insight ins : insights) {
            PdfPCell tipo = new PdfPCell(new Phrase(safe(ins.getTipo()), fb(8, DARK)));
            tipo.setBackgroundColor(LIGHT);
            tipo.setPadding(6);
            t.addCell(tipo);

            PdfPCell desc = new PdfPCell(new Phrase(safe(ins.getDescricao()), fn(8, new Color(15, 23, 42))));
            desc.setPadding(6);
            t.addCell(desc);
        }
        doc.add(t);
    }

    // Corrigido: parâmetro 'r' nunca usado removido
    private void adicionarFeedbackFicha(Document doc, FeedbackReuniao fb)
            throws DocumentException {
        if (fb == null) return;

        com.lowagie.text.Paragraph sub = new com.lowagie.text.Paragraph("Feedback Educativo", fb(10, BLUE));
        sub.setSpacingBefore(8);
        sub.setSpacingAfter(4);
        doc.add(sub);

        if (ok(fb.getProblemaIdentificado())) {
            PdfPTable t = tbl(1);
            PdfPCell c = new PdfPCell(new Phrase("Problema: " + fb.getProblemaIdentificado(), fn(9, new Color(15, 23, 42))));
            c.setBackgroundColor(LIGHT);
            c.setPadding(8);
            t.addCell(c);
            doc.add(t);
        }

        if (ok(fb.getAcaoDeMelhoria())) {
            com.lowagie.text.Paragraph acao = new com.lowagie.text.Paragraph(
                    "Ação de Melhoria: " + fb.getAcaoDeMelhoria(),
                    fn(9, new Color(15, 23, 42)));
            acao.setIndentationLeft(10);
            acao.setSpacingBefore(4);
            doc.add(acao);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private PdfPTable tbl(int cols) throws DocumentException {
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        t.setSpacingAfter(4);
        return t;
    }

    private void celula(PdfPTable t, String texto, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fn(8, new Color(15, 23, 42))));
        c.setBackgroundColor(bg);
        c.setPadding(6);
        t.addCell(c);
    }

    private Color corSentimento(Reuniao r) {
        if (r.getSentimento() == null) return LIGHT;
        return switch (r.getSentimento()) {
            case CRITICO -> new Color(254, 226, 226);
            case NEGATIVO -> new Color(254, 243, 199);
            case POSITIVO, OPORTUNIDADE_COMERCIAL -> new Color(209, 250, 229);
            default -> LIGHT;
        };
    }

    private Color corChurn(Reuniao r) {
        if (r.getRiscoChurn() == null) return LIGHT;
        return switch (r.getRiscoChurn()) {
            case ALTO  -> new Color(254, 226, 226);
            case MEDIO -> new Color(254, 243, 199);
            default    -> new Color(209, 250, 229);
        };
    }

    private Font fn(int size, Color color) {
        return new Font(Font.HELVETICA, size, Font.NORMAL, color);
    }

    private Font fb(int size, Color color) {
        return new Font(Font.HELVETICA, size, Font.BOLD, color);
    }

    private String safe(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private boolean ok(String s) {
        return s != null && !s.isBlank();
    }
}