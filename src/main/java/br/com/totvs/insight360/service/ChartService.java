package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Reuniao;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;

/**
 * Gera graficos JFreeChart como arrays de bytes PNG para insercao nos PDFs.
 */
@Service
@Slf4j
public class ChartService {

    // Paleta corporativa TOTVS
    private static final Color TOTVS_BLUE   = new Color(0,   87,  255);
    private static final Color TOTVS_CYAN   = new Color(0,   180, 230);
    private static final Color TOTVS_RED    = new Color(232, 56,  79);
    private static final Color TOTVS_ORANGE = new Color(245, 158, 11);
    private static final Color TOTVS_GREEN  = new Color(16,  185, 129);
    private static final Color TOTVS_GRAY   = new Color(108, 117, 125);
    private static final Color TOTVS_PURPLE = new Color(139, 92,  246);
    private static final Color TOTVS_AMBER  = new Color(251, 191, 36);

    private static final Color[] PALETTE = {
            TOTVS_BLUE, TOTVS_CYAN, TOTVS_ORANGE, TOTVS_GREEN,
            TOTVS_PURPLE, TOTVS_AMBER, TOTVS_RED, TOTVS_GRAY
    };

    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 11);
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 9);
    private static final Font TICK_FONT  = new Font("Arial", Font.PLAIN, 8);

    // ── Rosca: status das reunioes (COMPLETA/PARCIAL/INCOMPLETA) ─────

    public byte[] graficoStatus(List<Reuniao> reunioes) {
        long completas   = reunioes.stream().filter(Reuniao::isCompleta).count();
        long parciais    = reunioes.stream().filter(Reuniao::isParcial).count();
        long incompletas = reunioes.stream().filter(Reuniao::isIncompleta).count();
        if (completas + parciais + incompletas == 0) return null;

        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        if (completas   > 0) ds.setValue("Completas ("     + completas   + ")", completas);
        if (parciais    > 0) ds.setValue("Parciais ("      + parciais    + ")", parciais);
        if (incompletas > 0) ds.setValue("Insuficientes (" + incompletas + ")", incompletas);

        JFreeChart chart = ChartFactory.createPieChart(
                "Status das Reunioes", ds, true, false, false);

        PiePlot<?> plot = getPiePlot(chart);
        if (plot != null) {
            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(TITLE_FONT);
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setLabelFont(LABEL_FONT);
            plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
            plot.setLabelOutlinePaint(null);
            plot.setLabelShadowPaint(null);
            for (String key : ds.getKeys()) {
                String k = key.toLowerCase();
                if (k.startsWith("complet"))  plot.setSectionPaint(key, TOTVS_GREEN);
                if (k.startsWith("parciais")) plot.setSectionPaint(key, TOTVS_ORANGE);
                if (k.startsWith("insuf"))    plot.setSectionPaint(key, TOTVS_RED);
            }
        }
        return toBytes(chart, 400, 230);
    }

    // ── Pizza: distribuicao de sentimentos ───────────────────────────

    public byte[] graficoSentimentos(List<Reuniao> reunioes) {
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (Reuniao r : reunioes) {
            if (r.getSentimento() != null) {
                contagem.merge(r.getSentimentoFormatado(), 1L, Long::sum);
            }
        }
        if (contagem.isEmpty()) return null;
        contagem.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> ds.setValue(e.getKey() + " (" + e.getValue() + ")", e.getValue()));

        JFreeChart chart = ChartFactory.createPieChart(
                "Distribuicao de Sentimentos", ds, true, false, false);
        estilizarPie(chart, ds);

        PiePlot<?> plot = getPiePlot(chart);
        if (plot != null) {
            for (String key : ds.getKeys()) {
                String k = key.toLowerCase();
                if (k.startsWith("positivo"))     plot.setSectionPaint(key, TOTVS_GREEN);
                if (k.startsWith("negativo"))     plot.setSectionPaint(key, TOTVS_RED);
                if (k.startsWith("critico"))      plot.setSectionPaint(key, new Color(180, 0, 0));
                if (k.startsWith("oportunidade")) plot.setSectionPaint(key, TOTVS_BLUE);
                if (k.startsWith("misto"))        plot.setSectionPaint(key, TOTVS_ORANGE);
                if (k.startsWith("neutro"))       plot.setSectionPaint(key, TOTVS_GRAY);
            }
        }
        return toBytes(chart, 400, 230);
    }

    // ── Barras: categorias de reuniao ────────────────────────────────

    public byte[] graficoCategorias(List<Reuniao> reunioes) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (Reuniao r : reunioes) {
            String cat = r.getCategoriaPrincipal();
            if (cat != null && !cat.isBlank()) {
                contagem.merge(cat, 1L, Long::sum);
            }
        }
        if (contagem.isEmpty()) return null;

        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        contagem.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(e -> ds.addValue(e.getValue(), "Reunioes", truncar(e.getKey(), 18)));

        JFreeChart chart = ChartFactory.createBarChart(
                "Categorias de Reunioes", "Categoria", "Quantidade",
                ds, PlotOrientation.VERTICAL, false, true, false);
        estilizarBar(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        ((BarRenderer) plot.getRenderer()).setSeriesPaint(0, TOTVS_BLUE);
        return toBytes(chart, 480, 230);
    }

    // ── Pizza: risco de churn ────────────────────────────────────────

    public byte[] graficoChurn(List<Reuniao> reunioes) {
        long alto  = reunioes.stream().filter(r -> br.com.totvs.insight360.model.RiscoChurn.ALTO == r.getRiscoChurn()).count();
        long medio = reunioes.stream().filter(r -> br.com.totvs.insight360.model.RiscoChurn.MEDIO == r.getRiscoChurn()).count();
        long baixo = reunioes.stream().filter(r ->
                r.getRiscoChurn() == null || br.com.totvs.insight360.model.RiscoChurn.BAIXO == r.getRiscoChurn()).count();
        if (alto + medio + baixo == 0) return null;

        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        if (alto  > 0) ds.setValue("Alto ("  + alto  + ")", alto);
        if (medio > 0) ds.setValue("Medio (" + medio + ")", medio);
        if (baixo > 0) ds.setValue("Baixo (" + baixo + ")", baixo);

        JFreeChart chart = ChartFactory.createPieChart(
                "Risco de Churn", ds, true, false, false);

        PiePlot<?> plot = getPiePlot(chart);
        if (plot != null) {
            estilizarPie(chart, ds);
            for (String key : ds.getKeys()) {
                String k = key.toLowerCase();
                if (k.startsWith("alto"))  plot.setSectionPaint(key, TOTVS_RED);
                if (k.startsWith("medio")) plot.setSectionPaint(key, TOTVS_ORANGE);
                if (k.startsWith("baixo")) plot.setSectionPaint(key, TOTVS_GREEN);
            }
        }
        return toBytes(chart, 380, 220);
    }

    // ── Barras: prioridade comercial ─────────────────────────────────

    public byte[] graficoPrioridade(List<Reuniao> reunioes) {
        long alta  = reunioes.stream().filter(r -> "Alta".equalsIgnoreCase(r.getPrioridade())).count();
        long media = reunioes.stream().filter(r ->
                "Média".equalsIgnoreCase(r.getPrioridade()) || "Media".equalsIgnoreCase(r.getPrioridade())).count();
        long baixa = reunioes.stream().filter(r -> "Baixa".equalsIgnoreCase(r.getPrioridade())).count();
        if (alta + media + baixa == 0) return null;

        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        ds.addValue(alta,  "Reunioes", "Alta");
        ds.addValue(media, "Reunioes", "Media");
        ds.addValue(baixa, "Reunioes", "Baixa");

        JFreeChart chart = ChartFactory.createBarChart(
                "Prioridade Comercial", "Prioridade", "Quantidade",
                ds, PlotOrientation.VERTICAL, false, true, false);
        estilizarBar(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = criarRenderer(plot);
        renderer.setSeriesPaint(0, TOTVS_RED);
        renderer.setSeriesPaint(1, TOTVS_ORANGE);
        renderer.setSeriesPaint(2, TOTVS_GREEN);
        return toBytes(chart, 320, 210);
    }

    // ── Barras horizontais: produtos TOTVS ──────────────────────────

    public byte[] graficoProdutos(List<Reuniao> reunioes) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (Reuniao r : reunioes) {
            if (r.getProdutosIdentificados() != null && !r.getProdutosIdentificados().isBlank()) {
                for (String prod : r.getProdutosIdentificados().split(",")) {
                    String p = prod.trim();
                    if (!p.isBlank() && !p.equals("—")
                            && !p.toLowerCase().contains("não identificado")
                            && !p.toLowerCase().contains("nao identificado")) {
                        contagem.merge(p, 1L, Long::sum);
                    }
                }
            }
        }
        if (contagem.isEmpty()) return null;

        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        contagem.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> ds.addValue(e.getValue(), "Mencoes", truncar(e.getKey(), 22)));

        JFreeChart chart = ChartFactory.createBarChart(
                "Produtos TOTVS — Ranking", "Produto", "Mencoes",
                ds, PlotOrientation.HORIZONTAL, false, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(TITLE_FONT);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setOutlineVisible(false);

        plot.getDomainAxis().setTickLabelFont(TICK_FONT);
        plot.getRangeAxis().setTickLabelFont(TICK_FONT);

        BarRenderer renderer = criarRenderer(plot);
        renderer.setMaximumBarWidth(0.5);
        renderer.setSeriesPaint(0, TOTVS_BLUE);
        return toBytes(chart, 500, 280);
    }

    // ── Helpers de estilo ────────────────────────────────────────────

    private void estilizarPie(JFreeChart chart, DefaultPieDataset<String> ds) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(TITLE_FONT);
        PiePlot<?> plot = getPiePlot(chart);
        if (plot == null) return;
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(LABEL_FONT);
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        int i = 0;
        for (String key : ds.getKeys()) {
            plot.setSectionPaint(key, PALETTE[i % PALETTE.length]);
            i++;
        }
    }

    private void estilizarBar(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(TITLE_FONT);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setOutlineVisible(false);

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setTickLabelFont(TICK_FONT);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        plot.getRangeAxis().setTickLabelFont(TICK_FONT);

        BarRenderer renderer = criarRenderer(plot);
        renderer.setMaximumBarWidth(0.18);
        for (int i = 0; i < PALETTE.length; i++) {
            renderer.setSeriesPaint(i, PALETTE[i]);
        }
    }

    /**
     * Extrai e configura o BarRenderer do plot, centralizando a criacao do renderer
     * para eliminar duplicacao e o warning "possivel extracao de metodo".
     */
    private BarRenderer criarRenderer(CategoryPlot plot) {
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        return renderer;
    }

    private PiePlot<?> getPiePlot(JFreeChart chart) {
        if (chart.getPlot() instanceof PiePlot<?> p) return p;
        return null;
    }

    private byte[] toBytes(JFreeChart chart, int w, int h) {
        try {
            BufferedImage img = chart.createBufferedImage(w, h);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Falha ao converter grafico para PNG: {}", e.getMessage());
            return null;
        }
    }

    private String truncar(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "~" : s;
    }
}