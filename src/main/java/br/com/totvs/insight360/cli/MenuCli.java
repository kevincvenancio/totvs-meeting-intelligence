package br.com.totvs.insight360.cli;

import br.com.totvs.insight360.model.FeedbackReuniao;
import br.com.totvs.insight360.model.ImportacaoLote;
import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.service.CsvService;
import br.com.totvs.insight360.service.DadosDashboard;
import br.com.totvs.insight360.service.DashboardService;
import br.com.totvs.insight360.service.ImportacaoLoteService;
import br.com.totvs.insight360.service.PdfService;
import br.com.totvs.insight360.service.ResultadoImportacao;
import br.com.totvs.insight360.service.ReuniaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface de linha de comando (CLI) do TOTVS Meeting Intelligence.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused") // instanciada pelo Spring via @Component / CommandLineRunner
public class MenuCli implements CommandLineRunner {

    private final CsvService            csvService;
    private final DashboardService      dashboardService;
    private final PdfService            pdfService;
    private final ReuniaoService        reuniaoService;
    private final ImportacaoLoteService loteService;

    private final Scanner scanner = new Scanner(System.in);

    // ── Constantes de layout ──────────────────────────────────────────
    private static final String         SEP          = "─".repeat(72);
    private static final String         SEP2         = "═".repeat(72);
    private static final int            PARAGRAFO_W  = 68;  // largura fixa de parágrafos
    private static final DateTimeFormatter FMT        = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMTDT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Ponto de entrada ──────────────────────────────────────────────

    @Override
    public void run(String... args) {
        boasVindas();
        boolean rodando = true;
        while (rodando) {
            menuPrincipal();
            switch (ler("Opção").trim()) {
                case "1"  -> importarCsv();
                case "2"  -> verDashboard();
                case "3"  -> listarReunioes();
                case "4"  -> menuFiltros();
                case "5"  -> verReuniao();
                case "6"  -> verFeedback();
                case "7"  -> gerarPdf(); // CORREÇÃO: Nome corrigido para bater com o método
                case "8"  -> menuRelatorios();
                case "9"  -> listarFeedbacks();
                case "10" -> menuLotes();
                case "0"  -> { println("\n  Até logo. Obrigado por usar o TOTVS Insight360!\n"); rodando = false; }
                default   -> println("  Opcao invalida. Digite um numero do menu.");
            }
        }
        scanner.close();
    }

    // ── Menus ─────────────────────────────────────────────────────────

    private void boasVindas() {
        println("");
        println("  ╔════════════════════════════════════════════════════════════════════╗");
        println("  ║       TOTVS Insight360 — Inteligencia Comercial                   ║");
        println("  ║       Analise de Transcricoes de Reunioes TOTVS                   ║");
        println("  ╚════════════════════════════════════════════════════════════════════╝");
        println("");
    }

    private void menuPrincipal() {
        long total      = reuniaoService.contar();
        long analisadas = reuniaoService.contarAnalisadas();
        long lotes      = loteService.listarTodos().size();
        println("");
        println("  " + SEP2);
        println("  MENU PRINCIPAL  |  " + total + " reuniao(oes)  |  " + analisadas + " analisadas  |  " + lotes + " lote(s)");
        println("  " + SEP2);
        println("  1  ->  Importar CSV de transcricoes");
        println("  2  ->  Dashboard executivo");
        println("  3  ->  Listar todas as reunioes");
        println("  4  ->  Filtrar / Pesquisar reunioes");
        println("  5  ->  Ver analise detalhada de uma reuniao");
        println("  6  ->  Ver feedback educativo de uma reuniao");
        println("  7  ->  Gerar PDF de uma reuniao");
        println("  8  ->  Gerar relatório PDF (geral / por lote)");
        println("  9  ->  Listar todos os feedbacks");
        println("  10 ->  Gerenciar lotes de importacao");
        println("  0  ->  Sair");
        println("  " + SEP);
    }

    // ── 1. Importar CSV ───────────────────────────────────────────────

    private void importarCsv() {
        titulo("IMPORTAR CSV");
        println("  Informe o caminho completo do arquivo CSV.");
        println("  Exemplo: C:\\Users\\usuario\\Desktop\\transcricoes.csv");
        println("");
        String caminho = ler("Caminho do arquivo");
        File arquivo = new File(caminho.trim());

        if (!arquivo.exists()) { println("  Arquivo nao encontrado: " + caminho); return; }
        if (!arquivo.getName().endsWith(".csv")) { println("  O arquivo precisa ter extensao .csv"); return; }

        println("");
        println("  Processando " + arquivo.getName() + " ...");
        println("  (Isso pode levar alguns segundos dependendo do tamanho)");

        try {
            long inicio = System.currentTimeMillis();
            ResultadoImportacao resultado = csvService.processar(arquivo);
            long tempo = (System.currentTimeMillis() - inicio) / 1000;

            println("");
            println("  " + SEP);
            println("  Importacao concluida em " + tempo + "s");
            println("  " + SEP);
            if (resultado.lote() != null) {
                println(String.format("  %-28s: %s", "Lote criado",  resultado.lote().getCodigoLote()));
                println(String.format("  %-28s: %s", "Arquivo",      resultado.lote().getNomeArquivoOriginal()));
                println(String.format("  %-28s: %s", "Banco",        "H2 em arquivo (./data/)"));
            }
            println(String.format("  %-28s: %d", "Registros brutos",         resultado.totalEncontradas()));
            println(String.format("  %-28s: %d", "Reunioes validas",          resultado.processadas()));
            println(String.format("  %-28s: %d", "Transcricoes incompletas",  resultado.incompletas()));
            println(String.format("  %-28s: %d", "Duplicadas identificadas",  resultado.duplicadas()));
            println(String.format("  %-28s: %d", "Erros de parse",            resultado.erros()));

            // CORREÇÃO: Chamando o método correto errosDetalhe() gerado pelo Record/Getter
            if (resultado.errosDetalhe() != null && !resultado.errosDetalhe().isEmpty()) {
                println("  Detalhes dos erros:");
                resultado.errosDetalhe().forEach(e -> println("    - " + e));
            }
            println("  " + SEP);
            println("");
            println("  Deseja:");
            println("  1  ->  Ver reunioes deste lote");
            println("  2  ->  Gerar PDF deste lote");
            println("  3  ->  Voltar ao menu");
            String op = ler("Opcao").trim();
            if ("1".equals(op) && resultado.lote() != null) listarReunioesPorLote(resultado.lote());
            else if ("2".equals(op) && resultado.lote() != null) gerarPdfLote(resultado.lote());

        } catch (Exception e) {
            println("  Erro ao processar CSV: " + e.getMessage());
        }
    }

    // ── 2. Dashboard ──────────────────────────────────────────────────

    private void verDashboard() {
        titulo("DASHBOARD EXECUTIVO");

        List<Reuniao> todas = reuniaoService.listarAnalisadas();
        if (todas.isEmpty()) {
            println("  Nenhuma reuniao analisada. Importe um CSV primeiro (opcao 1).");
            return;
        }

        DadosDashboard d = dashboardService.getDados();

        println("  " + SEP);
        println("  RESUMO GERAL");
        println("  " + SEP);
        // CORREÇÃO: Acesso aos métodos do Record sem o prefixo "get"
        kpi("Reunioes analisadas",      "" + d.totalReunioes());
        kpi("Clientes unicos",          "" + d.totalClientes());
        kpi("Oportunidades ativas",     "" + d.totalOportunidades());
        kpi("Risco churn ALTO",         "" + d.totalChurnAlto());
        kpi("Risco churn MEDIO",        "" + d.totalChurnMedio());
        kpi("Score qualidade medio",    d.scoreQualidadeMedia() + "/100");
        kpi("Score comercial medio",    d.scoreComercialMedia() + "/100");
        kpi("Sentimento predominante",  d.sentimentoPredominante() != null ? d.sentimentoPredominante() : "N/A");

        long completas   = todas.stream().filter(Reuniao::isCompleta).count();
        long parciais    = todas.stream().filter(Reuniao::isParcial).count();
        long incompletas = todas.stream().filter(Reuniao::isIncompleta).count();
        println("");
        println("  " + SEP);
        println("  COMPLETUDE");
        println("  " + SEP);
        kpi("Completas",    "" + completas);
        kpi("Parciais",     "" + parciais);
        kpi("Incompletas",  "" + incompletas);

        if (d.topCategorias() != null && !d.topCategorias().isEmpty()) {
            println("");
            println("  " + SEP);
            println("  TOP CATEGORIAS DE PROBLEMAS");
            println("  " + SEP);
            int i = 1;
            for (Map.Entry<String, Long> e : d.topCategorias().entrySet())
                println("  " + i++ + ". " + e.getKey() + " — " + e.getValue() + " reunioes");
        }

        if (d.topProdutos() != null && !d.topProdutos().isEmpty()) {
            println("");
            println("  " + SEP);
            println("  PRODUTOS TOTVS MAIS CITADOS");
            println("  " + SEP);
            d.topProdutos().forEach((k, v) -> println("  - " + k + " — " + v + "x"));
        }

        if (d.topConcorrentes() != null && !d.topConcorrentes().isEmpty()) {
            println("");
            println("  " + SEP);
            println("  CONCORRENTES MENCIONADOS");
            println("  " + SEP);
            d.topConcorrentes().forEach((k, v) -> println("  - " + k + " — " + v + "x"));
        }

        if (d.topOportunidades() != null && !d.topOportunidades().isEmpty()) {
            println("");
            println("  " + SEP);
            println("  TOP OPORTUNIDADES (por score comercial)");
            println("  " + SEP);
            int i = 1;
            for (Reuniao r : d.topOportunidades())
                println("  " + i++ + ". " + nvl(r.getCliente())
                        + " — Score " + r.getScoreComercial() + "/100"
                        + (r.getData() != null ? " | " + r.getData().format(FMT) : ""));
        }

        if (d.topChurn() != null && !d.topChurn().isEmpty()) {
            println("");
            println("  " + SEP);
            println("  TOP RISCO DE CHURN");
            println("  " + SEP);
            for (Reuniao r : d.topChurn())
                println("  [" + r.getRiscoChurnLabel() + "] " + nvl(r.getCliente())
                        + (ok(r.getConcorrentesIdentificados())
                        ? " — Concorrente: " + r.getConcorrentesIdentificados() : ""));
        }
        println("");
    }

    // ── 3. Listar reuniões ────────────────────────────────────────────

    private void listarReunioes() {
        titulo("REUNIOES ANALISADAS");
        java.util.List<Reuniao> lista = reuniaoService.listarTodasOrdenadas();
        if (lista.isEmpty()) {
            println("  Nenhuma reuniao analisada. Importe um CSV primeiro (opcao 1).");
            return;
        }
        imprimirCabecalhoLista();
        for (Reuniao r : lista) imprimirLinhaReuniao(r);
        println("  " + SEP);
        println("  Total: " + lista.size() + " reunioes");
    }

    // ── 4. Menu de Filtros ────────────────────────────────────────────

    private void menuFiltros() {
        titulo("FILTRAR / PESQUISAR REUNIOES");
        println("  1  ->  Buscar por cliente (nome)");
        println("  2  ->  Filtrar por status (COMPLETA / PARCIAL / INCOMPLETA)");
        println("  3  ->  Filtrar por sentimento");
        println("  4  ->  Filtrar por prioridade");
        println("  5  ->  Listar somente reunioes criticas (churn ALTO)");
        println("  6  ->  Listar reunioes com oportunidades comerciais");
        println("  0  ->  Voltar");
        println("  " + SEP);

        switch (ler("Opcao").trim()) {
            case "1" -> buscarPorCliente();
            case "2" -> filtrarPorStatus();
            case "3" -> filtrarPorSentimento();
            case "4" -> filtrarPorPrioridade();
            case "5" -> listarChurnAlto();
            case "6" -> listarOportunidades();
            default  -> println("  Opcao invalida.");
        }
    }

    private void buscarPorCliente() {
        String termo = ler("Nome do cliente (parcial)");
        java.util.List<Reuniao> lista = reuniaoService.buscarPorCliente(termo.trim());
        exibirListaFiltrada(lista, "Resultados para cliente: " + termo);
    }

    private void filtrarPorStatus() {
        println("  Status: COMPLETA | PARCIAL | INCOMPLETA");
        String status = ler("Status").trim().toUpperCase();
        java.util.List<Reuniao> lista = reuniaoService.filtrarPorStatus(status);
        exibirListaFiltrada(lista, "Reunioes com status: " + status);
    }

    private void filtrarPorSentimento() {
        println("  Sentimentos: POSITIVO | NEUTRO | NEGATIVO | CRITICO | OPORTUNIDADE_COMERCIAL");
        String sent = ler("Sentimento").trim().toUpperCase();
        java.util.List<Reuniao> lista = reuniaoService.filtrarPorSentimento(sent);
        exibirListaFiltrada(lista, "Reunioes com sentimento: " + sent);
    }

    private void filtrarPorPrioridade() {
        println("  Prioridades: Alta | Media | Baixa");
        String prior = ler("Prioridade").trim();
        java.util.List<Reuniao> lista = reuniaoService.filtrarPorPrioridade(prior);
        exibirListaFiltrada(lista, "Reunioes com prioridade: " + prior);
    }

    private void listarChurnAlto() {
        java.util.List<Reuniao> lista = reuniaoService.listarChurnAlto();
        exibirListaFiltrada(lista, "Reunioes com churn ALTO");
    }

    private void listarOportunidades() {
        java.util.List<Reuniao> lista = reuniaoService.listarAnalisadas().stream()
                .filter(r -> ok(r.getOportunidades()))
                .sorted(Comparator.comparingInt(
                        (Reuniao r) -> r.getScoreComercial() != null ? r.getScoreComercial() : 0).reversed())
                .collect(Collectors.toList());
        exibirListaFiltrada(lista, "Reunioes com oportunidades comerciais");
    }

    private void exibirListaFiltrada(java.util.List<Reuniao> lista, String tituloFiltro) {
        println("");
        println("  " + SEP);
        println("  " + tituloFiltro.toUpperCase());
        println("  " + SEP);
        if (lista.isEmpty()) {
            println("  Nenhuma reuniao encontrada.");
            return;
        }
        imprimirCabecalhoLista();
        for (Reuniao r : lista) imprimirLinhaReuniao(r);
        println("  " + SEP);
        println("  Total: " + lista.size() + " reunioe(s)");
    }

    // ── 5. Ver análise detalhada ──────────────────────────────────────

    private void verReuniao() {
        titulo("ANALISE DETALHADA DE REUNIAO");
        String idStr = ler("ID da reuniao");
        try {
            Long id = Long.parseLong(idStr.trim());
            Optional<Reuniao> opt = reuniaoService.buscarPorId(id);
            if (opt.isEmpty()) { println("  Reuniao nao encontrada."); return; }

            Reuniao r = opt.get();
            java.util.List<Insight> insights = reuniaoService.listarInsightsPorReuniao(id);

            println("");
            println("  " + SEP2);
            println("  REUNIAO #" + r.getId() + " — " + nvl(r.getCliente()));
            println("  " + SEP2);
            campo("Data",           r.getData() != null ? r.getData().format(FMT) : "-");
            campo("Duracao",        r.getDuracaoDisplay());
            campo("Status Base",    nvl(r.getStatusMeetingOriginal()));
            campo("Segmento",       nvl(r.getSegmento()));
            campo("UF",             nvl(r.getUf()));
            campo("Formato",        nvl(r.getFormato()));
            campo("Categoria",      nvl(r.getCategoriaPrincipal()));
            campo("Status",         r.getStatusCompletude() != null ? r.getStatusCompletude().name() : "-");
            campo("Pontuacao",      r.getPontuacaoCompletude() != null ? r.getPontuacaoCompletude() + "/100" : "-");
            campo("Prioridade",     nvl(r.getPrioridade()));

            println("");
            println("  " + SEP);
            println("  ANALISE COMERCIAL");
            println("  " + SEP);
            campo("Score Qualidade",  r.getScoreQualidade() + "/100");
            campo("Score Comercial",  r.getScoreComercial() + "/100");
            campo("Sentimento",       r.getSentimentoFormatado());
            campo("Justificativa",    nvl(r.getSentimentoJustificativa()));
            campo("Risco Churn",      r.getRiscoChurnLabel());
            campo("Budget",           nvl(r.getBudgetIdentificado()));
            campo("Produtos TOTVS",   nvl(r.getProdutosIdentificados()));
            campo("Concorrentes",     nvl(r.getConcorrentesIdentificados()));
            if (ok(r.getMotivoIncompletude()))
                campo("Motivo Incompletude", r.getMotivoIncompletude());
            if (ok(r.getLocutoresIdentificados()))
                campo("Locutores", truncar(r.getLocutoresIdentificados(), 90));

            if (ok(r.getDoresIdentificadas())) {
                println("");
                println("  DORES IDENTIFICADAS:");
                for (String dor : r.getDoresIdentificadas().split(";"))
                    if (!dor.isBlank()) println("     - " + dor.trim());
            }

            if (ok(r.getOportunidades())) {
                println("");
                println("  OPORTUNIDADES:");
                for (String o : r.getOportunidades().split(";"))
                    if (!o.isBlank()) println("     - " + o.trim());
            }

            if (ok(r.getCategoriasPrincipais())) {
                println("");
                println("  CATEGORIAS: " + r.getCategoriasPrincipais().replace(";", " | "));
            }

            if (ok(r.getRecomendacaoFinal())) {
                println("");
                println("  " + SEP);
                println("  RECOMENDACAO EXECUTIVA:");
                println("  " + SEP);
                imprimirParagrafo(r.getRecomendacaoFinal());
            }

            if (!insights.isEmpty()) {
                println("");
                println("  " + SEP);
                println("  INSIGHTS (" + insights.size() + ")");
                println("  " + SEP);
                for (Insight ins : insights) {
                    println("  [" + ins.getPrioridade() + "] " + ins.getTipo());
                    println("     " + ins.getDescricao());
                    if (ok(ins.getTrechoOrigem()))
                        println("     Trecho: \"" + truncar(ins.getTrechoOrigem(), 80) + "\"");
                    println("     Confianca: " + ins.getConfianca() + "%");
                    println("");
                }
            }

        } catch (NumberFormatException e) {
            println("  ID invalido. Digite apenas o numero.");
        }
    }

    // ── 6. Ver feedback ───────────────────────────────────────────────

    private void verFeedback() {
        titulo("FEEDBACK EDUCATIVO");
        String idStr = ler("ID da reuniao");
        try {
            Long id = Long.parseLong(idStr.trim());
            Optional<Reuniao> optR = reuniaoService.buscarPorId(id);
            if (optR.isEmpty()) { println("  Reuniao nao encontrada."); return; }

            Optional<FeedbackReuniao> optFb = reuniaoService.buscarFeedbackPorReuniao(id);
            if (optFb.isEmpty()) { println("  Feedback nao disponivel para esta reuniao."); return; }

            FeedbackReuniao fb = optFb.get();
            Reuniao r          = optR.get();

            println("");
            println("  " + SEP2);
            println("  FEEDBACK EDUCATIVO — " + nvl(r.getCliente()));
            println("  Criticidade: " + nvl(String.valueOf(fb.getNivelCriticidade())));
            println("  " + SEP2);

            blocoFb("PROBLEMA IDENTIFICADO",       fb.getProblemaIdentificado());
            blocoFb("CATEGORIA",                   fb.getCategoriaProblema());
            blocoFb("SINAIS NA CONVERSA",          fb.getSinaisNaConversa());
            blocoFb("POR QUE PASSOU DESPERCEBIDO", fb.getMotivoNaoIdentificadoAntes());
            blocoFb("COMO IDENTIFICAR ANTES",      fb.getComoIdentificarAntes());
            blocoFb("PERGUNTAS RECOMENDADAS",      fb.getPerguntasRecomendadas());
            blocoFb("ACAO DE MELHORIA",            fb.getAcaoDeMelhoria());

            if (ok(fb.getMensagemEducativa())) {
                println("");
                println("  " + SEP);
                println("  APRENDIZADO DESTA REUNIAO:");
                println("  " + SEP);
                imprimirParagrafo(fb.getMensagemEducativa());
            }

        } catch (NumberFormatException e) {
            println("  ID invalido. Digite apenas o numero.");
        }
    }

    // ── 7. Gerar PDF individual ───────────────────────────────────────

    // CORREÇÃO: Corrigida a escrita de 'generarPdf' para 'gerarPdf'
    private void gerarPdf() {
        titulo("GERAR PDF DE REUNIAO");
        String idStr = ler("ID da reuniao");
        try {
            Long id = Long.parseLong(idStr.trim());
            println("  Gerando PDF...");
            File pdf = pdfService.gerarPdfReuniao(id, ".");
            println("  PDF gerado: " + pdf.getAbsolutePath());
        } catch (NumberFormatException e) {
            println("  ID invalido.");
        } catch (Exception e) {
            println("  Erro ao gerar PDF: " + e.getMessage());
        }
    }

    // ── 9. Listar feedbacks ───────────────────────────────────────────

    private void listarFeedbacks() {
        titulo("FEEDBACKS EDUCATIVOS");
        java.util.List<FeedbackReuniao> lista =
                reuniaoService.listarFeedbacksOrdenadosPorCriticidade();
        if (lista.isEmpty()) {
            println("  Nenhum feedback gerado ainda.");
            return;
        }
        println(String.format("  %-5s  %-30s  %-8s  %-35s",
                "ID", "CLIENTE", "CRITIC.", "PROBLEMA"));
        println("  " + SEP);
        for (FeedbackReuniao fb : lista) {
            println(String.format("  %-5s  %-30s  %-8s  %-35s",
                    fb.getReuniao().getId(),
                    truncar(nvl(fb.getReuniao().getCliente()), 30),
                    nvl(String.valueOf(fb.getNivelCriticidade())),
                    truncar(nvl(fb.getProblemaIdentificado()), 35)));
        }
        println("  " + SEP);
        println("  Use a opcao 6 para ver o feedback completo de uma reuniao.");
    }

    // ── Helpers de exibição ───────────────────────────────────────────

    private void imprimirCabecalhoLista() {
        println(String.format("  %-5s  %-24s  %-10s  %-7s  %-7s  %-22s  %-6s  %-11s",
                "ID", "CLIENTE", "DATA", "CHURN", "SCORE", "SENTIMENTO", "PRIOR.", "STATUS"));
        println("  " + SEP);
    }

    private void imprimirLinhaReuniao(Reuniao r) {
        println(String.format("  %-5s  %-24s  %-10s  %-7s  %-7s  %-22s  %-6s  %-11s",
                r.getId(),
                truncar(nvl(r.getCliente()), 24),
                r.getData() != null ? r.getData().format(FMT) : "-",
                r.getRiscoChurnLabel(),
                r.getScoreComercial() != null ? r.getScoreComercial() + "/100" : "-",
                truncar(r.getSentimentoFormatado(), 22),
                nvl(r.getPrioridade()),
                r.getStatusCompletude() != null ? r.getStatusCompletude().name() : "-"));
    }

    private String ler(String prompt) {
        System.out.print("\n  -> " + prompt + ": ");
        return scanner.nextLine();
    }

    private void println(String s) { System.out.println(s); }

    private void titulo(String t) {
        println("");
        println("  ╔════════════════════════════════════════════════════════════════════╗");
        println("  ║  " + padDir(t) + "║");
        println("  ╚════════════════════════════════════════════════════════════════════╝");
    }

    private void kpi(String label, String valor) {
        println(String.format("  %-35s  %s", label, valor));
    }

    private void campo(String label, String valor) {
        println(String.format("  %-24s: %s", label, valor));
    }

    private void blocoFb(String tit, String conteudo) {
        if (conteudo == null || conteudo.isBlank()) return;
        println("");
        println("  " + tit + ":");
        println("  " + SEP);
        imprimirParagrafo(conteudo);
    }

    /** Imprime parágrafo com largura fixa definida por {@link #PARAGRAFO_W}. */
    private void imprimirParagrafo(String texto) {
        for (String linha : texto.split("\n")) {
            if (linha.length() <= PARAGRAFO_W) {
                println("  " + linha);
            } else {
                String[] palavras = linha.split(" ");
                StringBuilder buf = new StringBuilder("  ");
                for (String p : palavras) {
                    if (buf.length() + p.length() > PARAGRAFO_W + 2) {
                        println(buf.toString());
                        buf = new StringBuilder("  " + p + " ");
                    } else {
                        buf.append(p).append(" ");
                    }
                }
                if (buf.length() > 2) println(buf.toString());
            }
        }
    }

    private String truncar(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "~" : s;
    }

    private String padDir(String s) {
        if (s.length() >= PARAGRAFO_W) return s.substring(0, PARAGRAFO_W);
        return s + " ".repeat(PARAGRAFO_W - s.length());
    }

    // ── 8. Menu de relatórios ─────────────────────────────────────────

    private void menuRelatorios() {
        titulo("GERAR RELATÓRIO PDF");
        println("  1  ->  Relatório consolidado (todos os lotes)");
        println("  2  ->  Relatório do último lote importado");
        println("  3  ->  Relatório de um lote específico");
        println("  0  ->  Voltar");
        String op = ler("Opcao").trim();
        switch (op) {
            case "1" -> gerarPdfGeral();
            case "2" -> loteService.ultimoLote().ifPresentOrElse(
                    this::gerarPdfLote,
                    () -> println("  Nenhum lote encontrado no banco."));
            case "3" -> {
                listarResumoDosLotes();
                String cod = ler("Codigo do lote (ex: Lote 001)").trim();
                loteService.buscarPorCodigo(cod).ifPresentOrElse(
                        this::gerarPdfLote,
                        () -> println("  Lote nao encontrado: " + cod));
            }
            default -> {}
        }
    }

    private void gerarPdfGeral() {
        titulo("RELATÓRIO CONSOLIDADO");
        long total = reuniaoService.contarAnalisadas();
        if (total == 0) { println("  Nao ha reunioes analisadas no banco."); return; }
        println("  Gerando relatorio executivo de todos os lotes (" + total + " reunioes)...");
        try {
            File pdf = pdfService.gerarPdfGeral(".");
            println("  PDF gerado: " + pdf.getAbsolutePath());
        } catch (Exception e) {
            println("  Erro ao gerar PDF: " + e.getMessage());
        }
    }

    private void gerarPdfLote(ImportacaoLote lote) {
        titulo("RELATÓRIO DO " + lote.getCodigoLote().toUpperCase());
        long total = reuniaoService.contarPorLote(lote.getId());
        if (total == 0) { println("  O lote nao possui reunioes validas para analise."); return; }
        println("  Arquivo : " + lote.getNomeArquivoOriginal());
        println("  Reunioes: " + total);
        println("  Gerando PDF...");
        try {
            File pdf = pdfService.gerarPdfPorLote(lote, ".");
            println("  PDF gerado: " + pdf.getAbsolutePath());
        } catch (Exception e) {
            println("  Erro ao gerar PDF do lote: " + e.getMessage());
        }
    }

    // ── 10. Gerenciar lotes ───────────────────────────────────────────

    private void menuLotes() {
        titulo("LOTES DE IMPORTACAO");
        println("  1  ->  Listar todos os lotes");
        println("  2  ->  Ver reunioes de um lote");
        println("  3  ->  Gerar PDF de um lote");
        println("  0  ->  Voltar");
        String op = ler("Opcao").trim();
        switch (op) {
            case "1" -> listarTodosOsLotes();
            case "2" -> {
                listarResumoDosLotes();
                String cod = ler("Codigo do lote").trim();
                loteService.buscarPorCodigo(cod).ifPresentOrElse(
                        this::listarReunioesPorLote,
                        () -> println("  Lote nao encontrado."));
            }
            case "3" -> {
                listarResumoDosLotes();
                String cod = ler("Codigo do lote").trim();
                loteService.buscarPorCodigo(cod).ifPresentOrElse(
                        this::gerarPdfLote,
                        () -> println("  Lote nao encontrado."));
            }
            default -> {}
        }
    }

    private void listarTodosOsLotes() {
        java.util.List<ImportacaoLote> lotes = loteService.listarTodos();
        if (lotes.isEmpty()) { println("  Nenhum lote importado ainda."); return; }
        println("");
        println(String.format("  %-10s  %-30s  %-16s  %-7s  %-7s  %-7s  %-9s",
                "LOTE", "ARQUIVO", "IMPORTADO EM", "BRUTOS", "VALIDAS", "DUPLIC.", "STATUS"));
        println("  " + SEP);
        for (ImportacaoLote l : lotes) {
            println(String.format("  %-10s  %-30s  %-16s  %-7d  %-7d  %-7d  %-9s",
                    l.getCodigoLote(),
                    truncar(l.getNomeArquivoOriginal(), 30),
                    l.getDataHoraImportacao() != null ? l.getDataHoraImportacao().format(FMTDT) : "-",
                    l.getTotalRegistrosBrutos(),
                    l.getTotalReunioesValidas(),
                    l.getTotalReunioesDuplicadas(),
                    nvl(l.getStatusProcessamento())));
        }
        println("  " + SEP);
        println("  Total: " + lotes.size() + " lote(s)");
    }

    private void listarResumoDosLotes() {
        java.util.List<ImportacaoLote> lotes = loteService.listarTodos();
        if (lotes.isEmpty()) { println("  Nenhum lote disponível."); return; }
        println("  Lotes disponíveis:");
        for (ImportacaoLote l : lotes) {
            println(String.format("    %-10s  %-28s  %d reunioes validas",
                    l.getCodigoLote(),
                    truncar(l.getNomeArquivoOriginal(), 28),
                    l.getTotalReunioesValidas()));
        }
        println("");
    }

    // CORREÇÃO: Método completado e estruturado devidamente
    private void listarReunioesPorLote(ImportacaoLote lote) {
        titulo("REUNIOES DO " + lote.getCodigoLote().toUpperCase());
        println("  Arquivo : " + lote.getNomeArquivoOriginal());
        println("  Importado em: " + (lote.getDataHoraImportacao() != null
                ? lote.getDataHoraImportacao().format(FMTDT) : "-"));
        println("");

        java.util.List<Reuniao> lista = reuniaoService.listarPorLote(lote.getId());
        if (lista.isEmpty()) { println("  Nenhuma reuniao neste lote."); return; }

        java.util.List<Reuniao> validas = lista.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getDuplicada()))
                .toList();

        imprimirCabecalhoLista();
        for (Reuniao r : validas) {
            imprimirLinhaReuniao(r);
        }
        println("  " + SEP);
        println("  Total de reunioes validas no lote: " + validas.size());
    }

    // ── Métodos utilitários obrigatórios para o funcionamento interno ──
    private boolean ok(String s) {
        return s != null && !s.isBlank();
    }

    private String nvl(String s) {
        return ok(s) ? s : "-";
    }
}