package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Reuniao;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Builder responsável por gerar páginas de capa para os relatórios PDF.
 * <p>
 * Extraído de {@link PdfService} para respeitar o Princípio da Responsabilidade Única:
 * geração de capas é uma preocupação distinta da orquestração geral do relatório.
 */
@Service
@RequiredArgsConstructor
public class PdfCapaBuilder {

    private static final Color DARK   = new Color(0,   20,  60);
    private static final Color BLUE   = new Color(0,   87,  255);
    private static final Color CYAN   = new Color(0,   180, 230);
    private static final Color WHITE  = new Color(255, 255, 255);

    private static final String TITULO_PARTE1 = "TOTVS";
    private static final String TITULO_PARTE2 = "Meeting Intelligence";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Gera a capa individual de uma reunião (capa azul escura com dados da reunião).
     *
     * @param doc documento iText aberto
     * @param w   writer do documento
     * @param r   reunião a ser exibida na capa
     */
    @SuppressWarnings("unused")
    public void gerarCapaReuniao(Document doc, PdfWriter w, Reuniao r)
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

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(90);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(160);

        addCellFundo(t);
        addCellSemBorda(t,
                "Relatório Individual de Inteligência Comercial",
                fn(13, new Color(148, 163, 184)), 10, 20);
        addCellDivisor(t);
        addCellSemBorda(t,
                "Reunião #" + r.getId() + "  —  " + safe(r.getCliente()),
                fb(11, CYAN), 10, 6);
        addCellSemBorda(t,
                "Data: " + (r.getData() != null ? r.getData().format(FMT) : "Não informada")
                        + "  |  Duração: " + r.getDuracaoDisplay()
                        + "  |  Status: " + safe(r.getStatusCompletude() != null ? r.getStatusCompletude().name() : null),
                fn(9, new Color(100, 116, 139)), 4, 6);
        addCellSemBorda(t,
                "Gerado em " + LocalDate.now().format(FMT),
                fn(8, new Color(71, 85, 105)), 20, 0);

        doc.add(t);
        doc.newPage();
    }

    /**
     * Gera a capa executiva do relatório geral com KPIs.
     *
     * @param doc   documento iText aberto
     * @param w     writer do documento
     * @param total total de reuniões no relatório
     */
    @SuppressWarnings("unused")
    public void gerarCapaExecutiva(Document doc, PdfWriter w, int total)
            throws DocumentException {
        PdfContentByte cb = w.getDirectContent();
        cb.setColorFill(DARK);
        cb.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
        cb.fill();
        cb.setColorFill(BLUE);
        cb.rectangle(0, 0, 12, PageSize.A4.getHeight());
        cb.fill();

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(90);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingBefore(180);

        addCellFundo(t);
        addCellSemBorda(t,
                "Relatório Executivo de Inteligência Comercial",
                fn(13, new Color(148, 163, 184)), 10, 20);
        addCellDivisor(t);
        addCellSemBorda(t,
                total + " reuniões analisadas  |  Gerado em " + LocalDate.now().format(FMT),
                fn(10, new Color(100, 116, 139)), 10, 6);

        doc.add(t);
        doc.newPage();
    }

    // ── Helpers internos ──────────────────────────────────────────────

    private void addCellFundo(PdfPTable t) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setBackgroundColor(new Color(0, 40, 100));
        c.setPadding(16);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph();
        p.add(new Phrase(TITULO_PARTE1 + " ", fb(28, WHITE)));
        p.add(new Phrase(TITULO_PARTE2, fn(28, CYAN)));
        p.setAlignment(Element.ALIGN_CENTER);
        c.addElement(p);
        t.addCell(c);
    }

    private void addCellSemBorda(PdfPTable t, String text, Font f, float before, float after) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(0);
        c.setPaddingTop(before);
        c.setPaddingBottom(after);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBackgroundColor(DARK);
        t.addCell(c);
    }

    private void addCellDivisor(PdfPTable t) {
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(3);
        c.setBackgroundColor(BLUE);
        c.setBorder(0);
        c.setPaddingTop(8);
        c.setPaddingBottom(8);
        t.addCell(c);
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
}