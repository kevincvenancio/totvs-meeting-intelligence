package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.model.RiscoChurn;
import br.com.totvs.insight360.model.Sentimento;
import br.com.totvs.insight360.model.StatusCompletude;
import br.com.totvs.insight360.repository.InsightRepository;
import br.com.totvs.insight360.repository.ReuniaoRepository;
import br.com.totvs.insight360.util.TextoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Serviço principal de análise de transcrições de reuniões TOTVS.
 * Responsabilidades:
 *  - Classificar completude (COMPLETA / PARCIAL / INCOMPLETA) via pontuação
 *  - Identificar produtos TOTVS, concorrentes e dores
 *  - Delegar cálculo de sentimento, churn e extração de insights aos serviços especializados
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnaliseTranscricaoService {

    private final ReuniaoRepository reuniaoRepository;
    private final InsightRepository insightRepository;
    private final SentimentoService sentimentoService;
    private final ChurnService churnService;
    private final InsightExtratorService insightExtratorService;

    // ── Dicionário de Produtos TOTVS ──────────────────────────────────
    private static final Map<String, String[]> PRODUTOS_TOTVS = new LinkedHashMap<>();
    static {
        PRODUTOS_TOTVS.put("TOTVS Protheus",          new String[]{"protheus","erp totvs","backoffice","módulo financeiro","módulo contábil"});
        PRODUTOS_TOTVS.put("TOTVS RM",                new String[]{"totvs rm","rm educacional","rm obras","meu rh","portal rh"});
        PRODUTOS_TOTVS.put("TOTVS Datasul",           new String[]{"datasul"});
        PRODUTOS_TOTVS.put("TOTVS Fluig",             new String[]{"fluig","bpm totvs","fluxo de trabalho totvs","portal corporativo totvs"});
        PRODUTOS_TOTVS.put("TOTVS BI / Analytics",    new String[]{"totvs bi","bi totvs","analytics totvs","dashboard totvs"});
        PRODUTOS_TOTVS.put("TOTVS Carol",             new String[]{"carol totvs","ia totvs","plataforma dados totvs"});
        PRODUTOS_TOTVS.put("TOTVS RH",                new String[]{"totvs rh","gestão de pessoas totvs","rh totvs","módulo rh totvs"});
        PRODUTOS_TOTVS.put("TOTVS Folha / DP",        new String[]{"folha de pagamento totvs","payroll totvs","departamento pessoal totvs"});
        PRODUTOS_TOTVS.put("TOTVS Agora (Ponto)",     new String[]{"totvs agora","ponto eletrônico totvs","registro de ponto totvs","agora totvs"});
        PRODUTOS_TOTVS.put("TOTVS CRM",               new String[]{"totvs crm","crm totvs"});
        PRODUTOS_TOTVS.put("TOTVS Techfin",           new String[]{"techfin","totvs pay"});
        PRODUTOS_TOTVS.put("TOTVS Varejo",            new String[]{"totvs varejo","pdv totvs","frente de caixa totvs"});
        PRODUTOS_TOTVS.put("TOTVS Saúde",             new String[]{"totvs saúde","prontuário totvs"});
        PRODUTOS_TOTVS.put("TOTVS Educacional",       new String[]{"totvs educacional","rm educacional","ies totvs"});
        PRODUTOS_TOTVS.put("TOTVS Logística",         new String[]{"wms totvs","tms totvs","logística totvs"});
        PRODUTOS_TOTVS.put("TOTVS Gestão Financeira", new String[]{"gestão financeira totvs","contas totvs","conciliação totvs"});
        PRODUTOS_TOTVS.put("MENTOR / Feeds",          new String[]{"mentor totvs","feeds totvs","avaliação de desempenho totvs"});
    }

    // ── Dicionário de Concorrentes ────────────────────────────────────
    private static final Map<String, String[]> CONCORRENTES = new LinkedHashMap<>();
    static {
        CONCORRENTES.put("Senior Sistemas",     new String[]{"senior sistemas","sênior sistemas","senior rh"});
        CONCORRENTES.put("SAP",                 new String[]{"sap s/4","s4hana","sap hana","sap erp"});
        CONCORRENTES.put("Oracle",              new String[]{"oracle erp","jd edwards","netsuite"});
        CONCORRENTES.put("Linx",                new String[]{"linx sistemas","linx pos"});
        CONCORRENTES.put("Omie",                new String[]{"omie erp"});
        CONCORRENTES.put("Sankhya",             new String[]{"sankhya erp","sankhya om"});
        CONCORRENTES.put("Microsoft Dynamics",  new String[]{"dynamics 365","microsoft dynamics","d365"});
        CONCORRENTES.put("Salesforce",          new String[]{"salesforce crm","salesforce platform"});
        CONCORRENTES.put("Domínio Sistemas",    new String[]{"dominio sistemas","dominius"});
        CONCORRENTES.put("CIGAM",               new String[]{"cigam erp"});
        CONCORRENTES.put("Mega Sistemas",       new String[]{"mega sistemas","mega erp"});
        CONCORRENTES.put("Ahgora",              new String[]{"ahgora ponto","ahgora sistema"});
    }


    // ── Dores padronizadas ─────────────────────────────────────────────
    // Formato: {palavra-chave, descrição da dor, prioridade}
    private static final String[][] DORES = {
            {"processo manual",              "Processo manual identificado",               "ALTA"},
            {"planilha excel",               "Uso excessivo de planilhas",                 "MEDIA"},
            {"retrabalho",                   "Retrabalho operacional",                     "ALTA"},
            {"não integra",                  "Falta de integração entre sistemas",         "ALTA"},
            {"falta integração",             "Falta de integração entre sistemas",         "ALTA"},
            {"não conseguimos medir",        "Falta de indicadores e visibilidade",        "ALTA"},
            {"falta de relatório",           "Ausência de relatórios adequados",           "MEDIA"},
            {"sem dashboard",                "Ausência de dashboard gerencial",            "MEDIA"},
            {"tomada de decisão difícil",    "Dificuldade na tomada de decisão",          "ALTA"},
            {"folha manual",                 "Folha de pagamento manual",                  "ALTA"},
            {"processo lento",               "Processos lentos e ineficientes",            "MEDIA"},
            {"demora muito",                 "Demora operacional excessiva",               "MEDIA"},
            {"sistema atual não atende",     "Sistema atual insuficiente",                 "ALTA"},
            {"suporte demora",               "Suporte com demora no atendimento",          "ALTA"},
            {"chamado em aberto",            "Chamado de suporte pendente",                "MEDIA"},
            {"implantação difícil",          "Dificuldades na implantação",                "ALTA"},
            {"ninguém responde",             "Falta de resposta do suporte",               "ALTA"},
            {"não funciona",                 "Funcionalidade com problemas",               "ALTA"},
            {"perda de dados",               "Perda ou inconsistência de dados",           "ALTA"},
            {"gestão de escalas",            "Dificuldade na gestão de escalas",           "ALTA"},
            {"banco de horas",               "Controle complexo de banco de horas",        "MEDIA"},
            {"lentidão no sistema",          "Lentidão ou problemas de performance",       "ALTA"},
            {"faturamento incorreto",        "Problemas no faturamento",                   "ALTA"},
            {"nota fiscal",                  "Problemas com emissão de notas fiscais",     "MEDIA"},
            {"gestão de estoque",            "Dificuldades na gestão de estoque",          "MEDIA"},
            {"falta automação",              "Falta de automação de processos",            "ALTA"},
            {"compliance",                   "Necessidade de compliance ou auditoria",     "MEDIA"},
            {"segurança de dados",           "Preocupações com segurança da informação",   "ALTA"},
            {"treinamento dos usuários",     "Necessidade de treinamento de usuários",     "BAIXA"},
            {"migração de dados",            "Necessidade de migração ou implantação",     "ALTA"},
    };

    // ── Indicadores de locutor por papel ─────────────────────────────
    private static final String[] KW_TOTVS = {
            "nossa solução", "nosso sistema", "nosso produto", "implantação",
            "proposta", "licença totvs", "contrato totvs", "demonstração", "demo totvs"
    };
    private static final String[] KW_CLIENTE = {
            "nossa empresa", "nosso processo", "nosso erp", "hoje usamos",
            "precisamos de", "temos dificuldade", "nossa dor", "a gente precisa"
    };

    // ── Indicadores de reunião incompleta ─────────────────────────────
    private static final String[] FRASES_INCOMPLETA = {
            "áudio falhou", "não dá para ouvir", "vamos remarcar",
            "caiu a ligação", "não consigo ouvir", "problemas de áudio",
            "encerrando a reunião agora", "não posso participar hoje"
    };

    // ── Categorias de reunião ─────────────────────────────────────────
    // Última posição de cada array é o rótulo da categoria
    private static final String[][] CATS_REUNIAO = {
            {"diagnóstico","discovery","levantamento de requisitos","quais são suas dores","Diagnóstico / Discovery"},
            {"demo","demonstração","apresentar o produto","ver o sistema na prática","Demonstração de Produto"},
            {"proposta","investimento","contrato","fechar negócio","negociação","Negociação Comercial"},
            {"suporte","chamado","incidente","bug reportado","erro no sistema","Suporte / Problema Técnico"},
            {"implantação","implantar","go live","parametrização","onboarding","Implantação / Onboarding"},
            {"pós-venda","acompanhamento","follow-up","ongoing","Pós-Venda / Acompanhamento"},
            {"renovação","renovar contrato","vencimento do contrato","Renovação / Retenção"},
            {"cancelar","não renovar","churn","risco de cancelamento","insatisfeito com a totvs","Churn / Risco de Cancelamento"},
            {"treinamento","treinar usuários","capacitação","Treinamento"},
            {"reunião interna","time totvs","equipe interna","alinhamento interno","Reunião Interna TOTVS"},
    };

    // ── Análise principal ─────────────────────────────────────────────

    /**
     * Analisa uma reunião, preenchendo todos os campos derivados e salvando no banco.
     *
     * @param reuniao entidade já persistida (com transcrição preenchida)
     */
    @Transactional
    public void analisarReuniao(Reuniao reuniao) {

        // Obter texto para análise — prefere a transcrição tratada
        String texto = reuniao.getTranscricaoTratada();
        if (texto == null || texto.isBlank()) texto = reuniao.getTranscricaoOriginal();
        if (texto == null || texto.isBlank()) {
            marcarIncompleta(reuniao);
            reuniaoRepository.save(reuniao);
            return;
        }

        String lower = texto.toLowerCase();
        int palavras = TextoUtils.contarPalavras(texto);

        // 1. Identificar locutores e seus papéis
        Map<String, Integer> locFalas = contarFalas(texto);
        int numLocutores = locFalas.size();
        String locutoresDesc = inferirPapeisLocutores(texto, locFalas);

        // 2. Calcular pontuação de completude e classificar
        int pontuacao = calcularPontuacaoCompletude(lower, palavras, numLocutores,
                reuniao.getData(), reuniao.getCliente());
        String completude = classificarCompletude(pontuacao, lower, palavras);
        reuniao.setStatusCompletude(StatusCompletude.valueOf(completude));
        reuniao.setPontuacaoCompletude(pontuacao);
        reuniao.setQuantidadeLocutores(numLocutores);

        if ("INCOMPLETA".equals(completude)) {
            reuniao.setMotivoIncompletude(gerarMotivoIncompletude(lower, palavras, numLocutores, reuniao.getDuracaoMinutos()));
            reuniao.setInsightParcial(extrairInsightParcial(lower));
        }

        // 3. Limpar intuições anteriores e preparar nova lista
        insightRepository.deleteByReuniaoId(reuniao.getId());
        List<Insight> intuicoes = new ArrayList<>();

        // 4. Identificar entidades
        List<String> produtos     = identificarProdutos(lower, texto, intuicoes, reuniao);
        List<String> concorrentes = identificarConcorrentes(lower, texto, intuicoes, reuniao);
        List<String> dores        = identificarDores(lower, texto, intuicoes, reuniao, completude);
        List<String> areas        = identificarAreas(lower);

        // 5. Budget
        String budget = identificarBudget(texto, intuicoes, reuniao);

        // 6. Risco de churn — delegado ao ChurnService
        String churn = churnService.calcular(lower, concorrentes, texto, intuicoes, reuniao);

        // 7. Oportunidades de venda cruzada (apenas para reuniões não-incompletas)
        List<String> oportunidades;
        if ("INCOMPLETA".equals(completude)) {
            oportunidades = new ArrayList<>();
        } else {
            oportunidades = insightExtratorService.extrairOportunidadesVendaCruzada(lower, intuicoes, reuniao);
        }

        // 8. Sentimento — delegado ao SentimentoService
        String[] sentResult;
        if ("INCOMPLETA".equals(completude)) {
            sentResult = new String[]{"NEUTRO", "Dados insuficientes para análise de sentimento."};
        } else {
            sentResult = sentimentoService.calcular(lower, dores, concorrentes, churn, oportunidades);
        }

        // 9. Scores de qualidade e comercial
        int scoreQ = calcularScoreQualidade(palavras, produtos, dores, budget, numLocutores);
        int scoreC = "INCOMPLETA".equals(completude) ? 0
                : calcularScoreComercial(dores, produtos, concorrentes, budget, oportunidades, churn);

        // 10. Categorias e prioridade
        List<String> catProblemas = identificarCategoriasProblemas(lower);
        String catReuniao = identificarCategoriaReuniao(lower, completude);
        String prioridade = calcularPrioridade(sentResult[0], churn, scoreC, oportunidades, dores, completude);

        // 11. Textos derivados
        String tema    = inferirTema(catReuniao, produtos, dores);
        String resumo  = gerarResumo(palavras, completude, catReuniao, sentResult[0], numLocutores, reuniao.getDuracaoDisplay());
        String pontos  = listarPontosPrincipais(dores, produtos, concorrentes, oportunidades);
        String recFinal = gerarRecomendacao(concorrentes, churn, oportunidades, budget, scoreC, completude);

        // 12. Salvar intuições e atualizar reunião
        if (!intuicoes.isEmpty()) {
            insightRepository.saveAll(intuicoes);
        }

        reuniao.setProdutosIdentificados(join(produtos, ", "));
        reuniao.setConcorrentesIdentificados(join(concorrentes, ", "));
        reuniao.setDoresIdentificadas(join(dores, "; "));
        reuniao.setOportunidades(join(oportunidades, "; "));
        reuniao.setAreasInternas(join(areas, ", "));
        reuniao.setBudgetIdentificado(budget != null ? budget : "Não identificado");
        reuniao.setRiscoChurn(RiscoChurn.valueOf(churn));
        reuniao.setSentimento(Sentimento.valueOf(sentResult[0]));
        reuniao.setSentimentoJustificativa(sentResult[1]);
        reuniao.setScoreQualidade(scoreQ);
        reuniao.setScoreComercial(scoreC);
        reuniao.setCategoriasPrincipais(join(catProblemas, "; "));
        reuniao.setCategoriaReuniao(catReuniao);
        reuniao.setPrioridade(prioridade);
        reuniao.setTemaReuniao(tema);
        reuniao.setResumoReuniao(resumo);
        reuniao.setPontosPrincipais(pontos);
        reuniao.setLocutoresIdentificados(locutoresDesc);
        reuniao.setRecomendacaoFinal(recFinal);
        reuniao.setAnalisada(true);
        reuniao.setQuantidadePalavras(palavras);

        // Atualizar nome do cliente a partir da unidade TOTVS
        if (reuniao.getNomeUnidade() != null && !reuniao.getNomeUnidade().isBlank()) {
            reuniao.setCliente(reuniao.getNomeUnidade());
        }

        reuniaoRepository.save(reuniao);

        log.info("Reunião {} | {} ({} pts) | {} | scoreC={} | churn={} | sent={}",
                reuniao.getId(), completude, pontuacao, catReuniao, scoreC, churn, sentResult[0]);
    }

    // ── Locutores ─────────────────────────────────────────────────────

    /**
     * Conta quantas falas cada locutor tem na transcrição.
     * Retorna mapa ordenado por quantidade de falas (decrescente).
     */
    private Map<String, Integer> contarFalas(String texto) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        // Corrigido: removido [\\d] redundante — usar \d diretamente
        Pattern p = Pattern.compile("\\[LOCUTOR (\\d+)]:");
        Matcher m = p.matcher(texto);
        while (m.find()) {
            contagem.merge(m.group(1), 1, Integer::sum);
        }
        return contagem.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /**
     * Infere o papel provável de cada locutor (TOTVS vs Cliente) com base no vocabulário.
     *
     * @return descrição textual dos locutores e suas funções
     */
    private String inferirPapeisLocutores(String texto, Map<String, Integer> locFalas) {
        if (locFalas.isEmpty()) return "Nenhum locutor identificado";

        Pattern falaPattern = Pattern.compile(
                "\\[LOCUTOR (\\d+)]:\\s*(.+?)(?=\\[LOCUTOR|$)", Pattern.DOTALL);

        Map<String, List<String>> locFrasesMap = new HashMap<>();
        Matcher m = falaPattern.matcher(texto);
        while (m.find()) {
            String loc  = m.group(1);
            String fala = m.group(2).toLowerCase().trim();
            locFrasesMap.computeIfAbsent(loc, k -> new ArrayList<>()).add(fala);
        }

        StringBuilder sb = new StringBuilder();
        for (String loc : locFalas.keySet()) {
            List<String> frases = locFrasesMap.getOrDefault(loc, Collections.emptyList());
            String todasFrases  = String.join(" ", frases);
            String role = classificarPapelLocutor(todasFrases);
            sb.append("LOCUTOR ").append(loc).append(" — ").append(role)
                    .append(" (").append(locFalas.get(loc)).append(" falas); ");
        }
        return sb.toString().trim();
    }

    /** Classifica a função de um locutor com base em seu vocabulário. */
    private String classificarPapelLocutor(String todasFrases) {
        int scoreTotvs   = 0;
        int scoreCliente = 0;
        for (String kw : KW_TOTVS)   if (todasFrases.contains(kw)) scoreTotvs++;
        for (String kw : KW_CLIENTE) if (todasFrases.contains(kw)) scoreCliente++;

        if      (scoreTotvs > scoreCliente && scoreTotvs >= 2)   return "provável Consultor TOTVS";
        else if (scoreCliente > scoreTotvs && scoreCliente >= 2) return "provável Cliente";
        else if (scoreTotvs > 0)                                 return "Participante (possível TOTVS)";
        else if (scoreCliente > 0)                               return "Participante (possível Cliente)";
        else                                                     return "Participante não identificado";
    }

    // ── Pontuação e Completude ─────────────────────────────────────────

    /**
     * Calcula a pontuação de completude (0-100) com base em critérios objetivos.
     */
    private int calcularPontuacaoCompletude(String lower, int palavras,
                                            int locutores,
                                            java.time.LocalDate data, String cliente) {
        int pts = 0;

        if (cliente != null && !cliente.isBlank() && !cliente.startsWith("Cliente ")) pts += 20;
        if (data != null) pts += 15;

        if (palavras > 300) pts += 20;
        else if (palavras > 100) pts += 10;

        if (locutores >= 2)      pts += 15;
        else if (locutores == 1) pts += 5;

        boolean temDor = Arrays.stream(DORES).anyMatch(d -> lower.contains(d[0]));
        if (temDor) pts += 10;

        boolean temSolucao = TextoUtils.contemQualquer(lower,
                "sistema", "solução", "produto", "proposta", "implantação",
                "contrato", "módulo", "encaminhamento", "próximo passo", "follow");
        if (temSolucao) pts += 10;

        return Math.min(100, pts);
    }

    /**
     * Classifica a reunião com base na pontuação de completude.
     * 80-100: COMPLETA / 45-79: PARCIAL / 0-44: INCOMPLETA
     */
    private String classificarCompletude(int pontuacao, String lower, int palavras) {
        for (String f : FRASES_INCOMPLETA) {
            if (lower.contains(f) && palavras < 150) return "INCOMPLETA";
        }
        if (palavras < 50) return "INCOMPLETA";
        if (pontuacao >= 80) return "COMPLETA";
        if (pontuacao >= 45) return "PARCIAL";
        return "INCOMPLETA";
    }

    private String gerarMotivoIncompletude(String lower, int palavras, int locutores, Integer durMin) {
        List<String> motivos = new ArrayList<>();
        if (palavras < 50)   motivos.add("Transcrição com apenas " + palavras + " palavras — conteúdo insuficiente");
        else if (palavras < 150) motivos.add("Transcrição curta (" + palavras + " palavras)");
        if (locutores < 2)   motivos.add("Apenas " + locutores + " locutor(es) — reunião possivelmente cortada");
        if (durMin != null && durMin == 1) motivos.add("Duração inferior a 2 minutos");
        for (String f : FRASES_INCOMPLETA) {
            if (lower.contains(f)) { motivos.add("Indicativo de problema técnico: \"" + f + "\""); break; }
        }
        return motivos.isEmpty()
                ? "Contexto insuficiente para análise confiável"
                : String.join("; ", motivos);
    }

    private String extrairInsightParcial(String lower) {
        if (TextoUtils.contemQualquer(lower, "suporte", "chamado", "erro", "bug"))     return "Indício de suporte técnico";
        if (TextoUtils.contemQualquer(lower, "demo", "demonstração", "apresentação"))  return "Possível demonstração de produto";
        if (TextoUtils.contemQualquer(lower, "contrato", "proposta", "valor"))         return "Indício de negociação comercial";
        if (TextoUtils.contemQualquer(lower, "treinamento", "treinar", "capacitação")) return "Possível sessão de treinamento";
        return "Sem insight parcial — transcrição insuficiente";
    }

    /** Motivo fixo extraído como constante para evitar warning "value is always same". */
    private static final String MOTIVO_TRANSCRICAO_AUSENTE = "Transcrição ausente ou vazia";

    private void marcarIncompleta(Reuniao r) {
        r.setStatusCompletude(StatusCompletude.INCOMPLETA);
        r.setPontuacaoCompletude(0);
        r.setMotivoIncompletude(MOTIVO_TRANSCRICAO_AUSENTE);
        r.setInsightParcial("Não aplicável");
        r.setCategoriaReuniao("Reunião Incompleta / Sem Conteúdo Útil");
        r.setPrioridade("Baixa");
        r.setSentimento(Sentimento.NEUTRO);
        r.setSentimentoJustificativa("Dados insuficientes para análise de sentimento.");
        r.setScoreQualidade(0);
        r.setScoreComercial(0);
        r.setAnalisada(true);
        r.setQuantidadePalavras(0);
        r.setRiscoChurn(RiscoChurn.BAIXO);
    }

    // ── Identificação de entidades ─────────────────────────────────────

    private List<String> identificarProdutos(String lower, String texto,
                                             List<Insight> intuicoes, Reuniao r) {
        List<String> lista = new ArrayList<>();
        for (Map.Entry<String, String[]> e : PRODUTOS_TOTVS.entrySet()) {
            for (String kw : e.getValue()) {
                if (lower.contains(kw) && !lista.contains(e.getKey())) {
                    lista.add(e.getKey());
                    adicionarInsight(intuicoes, r,
                            "Produto TOTVS identificado",
                            "Produto " + e.getKey() + " mencionado na transcrição",
                            "MEDIA",
                            TextoUtils.extrairTrecho(texto, kw, 80),
                            90);
                    break;
                }
            }
        }
        return lista;
    }

    private List<String> identificarConcorrentes(String lower, String texto,
                                                 List<Insight> intuicoes, Reuniao r) {
        List<String> lista = new ArrayList<>();
        for (Map.Entry<String, String[]> e : CONCORRENTES.entrySet()) {
            for (String kw : e.getValue()) {
                if (lower.contains(kw) && !lista.contains(e.getKey())) {
                    lista.add(e.getKey());
                    adicionarInsight(intuicoes, r,
                            "Concorrente identificado",
                            e.getKey() + " mencionado como possível alternativa",
                            "ALTA",
                            TextoUtils.extrairTrecho(texto, kw, 80),
                            92);
                    break;
                }
            }
        }
        return lista;
    }

    private List<String> identificarDores(String lower, String texto,
                                          List<Insight> intuicoes, Reuniao r,
                                          String completude) {
        List<String> lista = new ArrayList<>();
        if ("INCOMPLETA".equals(completude)) return lista;

        for (String[] d : DORES) {
            if (lower.contains(d[0]) && !lista.contains(d[1])) {
                lista.add(d[1]);
                adicionarInsight(intuicoes, r,
                        "Dor identificada",
                        d[1],
                        d[2],
                        TextoUtils.extrairTrecho(texto, d[0], 100),
                        85);
            }
        }
        return lista;
    }

    private List<String> identificarAreas(String lower) {
        List<String> areas = new ArrayList<>();
        if (lower.contains("financeiro") || lower.contains("contas a pagar"))     areas.add("Área Financeira");
        if (lower.contains("recursos humanos") || lower.contains(" rh "))         areas.add("RH / Departamento Pessoal");
        if (lower.contains(" ti ") || lower.contains("tecnologia da informação"))  areas.add("TI / Tecnologia");
        if (lower.contains("área comercial") || lower.contains("equipe comercial")) areas.add("Área Comercial");
        if (lower.contains("logística") || lower.contains("estoque"))             areas.add("Logística / Estoque");
        if (lower.contains("suporte técnico") || lower.contains("help desk"))     areas.add("Suporte / CS");
        return areas;
    }

    private String identificarBudget(String texto, List<Insight> intuicoes, Reuniao r) {
        Pattern p = Pattern.compile(
                "R\\$\\s*[\\d.,]+(?:\\s*(?:mil|k|milhão|milhões))?|\\d+\\s*(?:mil|k|milhões)(?:\\s+reais)?",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            String val = m.group();
            adicionarInsight(intuicoes, r,
                    "Budget identificado",
                    "Valor financeiro mencionado: " + val,
                    "ALTA",
                    TextoUtils.extrairTrecho(texto, val, 100),
                    95);
            return val;
        }
        return null;
    }

    // ── Scores ─────────────────────────────────────────────────────────

    private int calcularScoreQualidade(int palavras, List<String> produtos,
                                       List<String> dores, String budget, int locutores) {
        int s = 0;
        if (palavras > 500)      s += 20; else if (palavras > 200) s += 10; else s += 5;
        if (!produtos.isEmpty()) s += 20;
        if (!dores.isEmpty())    s += 20;
        if (budget != null)      s += 15;
        if (locutores >= 2)      s += 15;
        if (palavras > 1000)     s += 10;
        return s;
    }

    private int calcularScoreComercial(List<String> dores, List<String> produtos,
                                       List<String> concorrentes, String budget,
                                       List<String> oportunidades, String churn) {
        int s = 0;
        if (!dores.isEmpty())         s += 25;
        if (!produtos.isEmpty())      s += 20;
        if (!concorrentes.isEmpty())  s += 10;
        if (budget != null)           s += 20;
        if (!oportunidades.isEmpty()) s += 15;
        if ("ALTO".equals(churn))     s = Math.max(s - 10, 10);
        return Math.min(100, s);
    }

    // ── Categorias ─────────────────────────────────────────────────────

    private List<String> identificarCategoriasProblemas(String lower) {
        List<String> cats = new ArrayList<>();
        if (TextoUtils.contemQualquer(lower, "processo manual", "planilha excel", "retrabalho"))
            cats.add("Processo manual / Retrabalho");
        if (TextoUtils.contemQualquer(lower, "não integra", "falta integração", "sistema separado"))
            cats.add("Falta de integração");
        if (TextoUtils.contemQualquer(lower, "falta de relatório", "sem dashboard", "sem indicador"))
            cats.add("Falta de indicadores");
        if (TextoUtils.contemQualquer(lower, "folha manual", "departamento pessoal manual"))
            cats.add("Problemas de RH / Folha");
        if (TextoUtils.contemQualquer(lower, "orçamento", "budget", "custo do projeto", "roi"))
            cats.add("Decisão financeira");
        if (TextoUtils.contemQualquer(lower, "cancelar contrato", "não renovar", "trocar de fornecedor"))
            cats.add("Risco de churn");
        boolean temConc = CONCORRENTES.entrySet().stream()
                .anyMatch(e -> Arrays.stream(e.getValue()).anyMatch(lower::contains));
        if (temConc) cats.add("Pressão competitiva");
        if (TextoUtils.contemQualquer(lower, "suporte demora", "chamado em aberto", "ninguém responde"))
            cats.add("Problema de atendimento");
        return cats;
    }

    private String identificarCategoriaReuniao(String lower, String completude) {
        if ("INCOMPLETA".equals(completude)) return "Reunião Incompleta / Sem Conteúdo Útil";
        for (String[] cat : CATS_REUNIAO) {
            String label = cat[cat.length - 1];
            String[] kws = Arrays.copyOf(cat, cat.length - 1);
            if (TextoUtils.contemQualquer(lower, kws)) return label;
        }
        return "Reunião Técnica / Geral";
    }

    private String calcularPrioridade(String sentimento, String churn, int scoreC,
                                      List<String> oportunidades, List<String> dores,
                                      String completude) {
        if ("INCOMPLETA".equals(completude)) return "Baixa";
        if ("ALTO".equals(churn) || "CRITICO".equals(sentimento)) return "Alta";
        if (scoreC >= 70 || !oportunidades.isEmpty()) return "Alta";
        if ("NEGATIVO".equals(sentimento) || scoreC >= 40 || !dores.isEmpty()) return "Média";
        return "Baixa";
    }

    // ── Textos derivados ───────────────────────────────────────────────

    private String inferirTema(String cat, List<String> produtos, List<String> dores) {
        if (!produtos.isEmpty() && !dores.isEmpty())
            return cat + " — produtos: " + String.join(", ", produtos);
        if (!produtos.isEmpty())
            return "Discussão sobre " + produtos.get(0);
        if (!dores.isEmpty())
            return "Mapeamento de problemas: " + dores.get(0);
        return cat;
    }

    private String gerarResumo(int palavras, String completude, String cat,
                               String sent, int locutores, String duracao) {
        if ("INCOMPLETA".equals(completude))
            return "Reunião com conteúdo insuficiente para análise completa (" + palavras
                    + " palavras). Categoria inferida: " + cat + ".";
        return "Reunião de " + duracao + " categorizada como \"" + cat + "\" com "
                + locutores + " locutor(es). Sentimento predominante: "
                + sent.toLowerCase().replace("_", " ") + ". Transcrição: " + palavras + " palavras.";
    }

    private String listarPontosPrincipais(List<String> dores, List<String> produtos,
                                          List<String> concorrentes, List<String> oportunidades) {
        List<String> pts = new ArrayList<>();
        if (!dores.isEmpty())
            pts.add("Dores: " + String.join(", ", dores.subList(0, Math.min(3, dores.size()))));
        if (!produtos.isEmpty())
            pts.add("Produtos: " + String.join(", ", produtos));
        if (!concorrentes.isEmpty())
            pts.add("Concorrentes: " + String.join(", ", concorrentes));
        if (!oportunidades.isEmpty())
            pts.add("Oportunidades: " + oportunidades.get(0));
        return pts.isEmpty() ? "Nenhum ponto específico identificado" : String.join("; ", pts);
    }

    private String gerarRecomendacao(List<String> concorrentes,
                                     String churn, List<String> oportunidades,
                                     String budget, int score, String completude) {
        if ("INCOMPLETA".equals(completude))
            return "Reunião incompleta — revisar transcrição original ou solicitar reprocessamento.";

        StringBuilder rec = new StringBuilder();
        if ("ALTO".equals(churn))
            rec.append("ALERTA: Risco de churn ALTO. Acionar Customer Success imediatamente. ");
        if (!concorrentes.isEmpty())
            rec.append("Concorrente identificado (").append(String.join(", ", concorrentes))
                    .append("). Preparar argumentação de diferenciação TOTVS. ");
        if (budget != null && !oportunidades.isEmpty())
            rec.append("Budget identificado (").append(budget).append("). Priorizar proposta. ");
        if (!oportunidades.isEmpty())
            rec.append(oportunidades.get(0)).append(". ");
        if (score >= 70)
            rec.append("Alta prioridade comercial — score ").append(score).append("/100.");
        else if (score >= 40)
            rec.append("Oportunidade moderada. Agendar follow-up.");
        else
            rec.append("Baixa informação comercial. Realizar novo contato para mapear dores.");

        return rec.toString().trim();
    }

    // ── Helpers internos ──────────────────────────────────────────────

    private void adicionarInsight(List<Insight> intuicoes, Reuniao r, String tipo,
                                  String desc, String prio, String trecho, int conf) {
        insightExtratorService.adicionarInsight(intuicoes, r, tipo, desc, prio, trecho, conf);
    }

    private String join(List<String> list, String sep) {
        return (list == null || list.isEmpty()) ? "" : String.join(sep, list);
    }
}