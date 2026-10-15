package br.com.totvs.hermes.chat;

import br.com.totvs.hermes.dao.ClienteDao;
import br.com.totvs.hermes.dao.ComentarioReuniaoDao;
import br.com.totvs.hermes.dao.PlanoAcaoDao;
import br.com.totvs.hermes.dao.ReuniaoDao;
import br.com.totvs.hermes.dto.filtro.FiltroPlanoAcao;
import br.com.totvs.hermes.model.Cliente;
import br.com.totvs.hermes.model.ComentarioReuniao;
import br.com.totvs.hermes.model.PlanoAcao;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.model.RiscoChurn;
import br.com.totvs.hermes.model.Sentimento;
import br.com.totvs.hermes.model.StatusCliente;
import br.com.totvs.hermes.model.StatusPlanoAcao;
import br.com.totvs.hermes.repository.IndicadoresReuniao;
import br.com.totvs.hermes.repository.ReuniaoRepository;
import br.com.totvs.hermes.service.ClienteService;
import br.com.totvs.hermes.service.DadosDashboard;
import br.com.totvs.hermes.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Monta, a cada pergunta, o bloco {@code <contexto>} com dados reais do banco.
 * <p>
 * O modelo nunca recebe a base inteira: só indicadores globais e os registros que
 * a pergunta pede (por ID, por cliente ou por tema). Alvo: menos de ~6.000 tokens.
 * Tudo que nasce de transcrição/comentário vai entre {@code <dado>} e sanitizado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContextoHermesService {

    /**
     * ~3.000 tokens. A cota grátis do Groq é de 8.000 tokens POR MINUTO (contexto + prompt +
     * resposta); com 20.000 caracteres, duas perguntas seguidas já estouravam o limite.
     */
    static final int MAX_CARACTERES_CONTEXTO = 12_000;
    static final int MAX_TRECHO_TRANSCRICAO = 1_500;
    private static final int TOP = 10;
    private static final int MAX_IDS_POR_PERGUNTA = 3;
    private static final long CACHE_GLOBAIS_MS = 60_000;

    private static final Pattern PADRAO_ID = Pattern.compile(
            "(?iu)(?:#\\s*|reuni[aã]o\\s*(?:n[º°o]?\\.?\\s*)?#?\\s*)(\\d{1,9})");
    private static final Pattern PADRAO_ID_PLANO = Pattern.compile(
            "(?iu)plano(?:\\s+de\\s+a[cç][aã]o)?\\s*(?:n[º°o]?\\.?\\s*)?#?\\s*(\\d{1,9})");
    private static final Pattern ROTULO_REUNIAO = Pattern.compile("REUNIÃO #(\\d+)");
    private static final Pattern ROTULO_PLANO = Pattern.compile("PLANO #(\\d+)");
    private static final Pattern ROTULO_CLIENTE = Pattern.compile("CLIENTE #(\\d+)");

    private static final Pattern PADRAO_CLIENTE = Pattern.compile(
            "(?iu)\\b(?:cliente|empresa|conta)\\s+(?:d[aeo]s?\\s+)?([\\p{L}\\p{N}&.'\\-]+(?:\\s+[\\p{L}\\p{N}&.'\\-]+){0,3})");

    private static final Set<String> PALAVRAS_PARADA = Set.of(
            "com", "de", "da", "do", "das", "dos", "que", "mais", "maior", "menor", "em", "no", "na",
            "esta", "está", "estão", "estao", "tem", "têm", "teve", "possui", "foi", "é", "e", "ou",
            "atual", "este", "esse", "essa", "aqui", "ativo", "ativos", "risco", "para", "por",
            "qual", "quais", "como", "sobre", "a", "o", "as", "os", "um", "uma", "x", "y");

    static final String MAPA_DE_TELAS = """
            TELAS DO SISTEMA (use para orientar o usuário):
            - /painel — Painel executivo: totais, sentimento, situação dos planos, produtos e concorrentes mais citados, rankings de maior risco de churn e maiores oportunidades; botão para baixar o relatório executivo em PDF.
            - /reunioes — lista de reuniões analisadas com busca (cliente, vendedor, identificador) e filtros (sentimento, risco de churn, prioridade, completude, período).
            - /reunioes/:id — detalhe da reunião: resumo, pontos principais, dores, oportunidades, feedback educativo, recomendação final, ficha, produtos e concorrentes citados, planos de ação, comentários e PDF da reunião.
            - /clientes — carteira de clientes: busca por razão social ou CNPJ, filtro por status, reuniões vinculadas.
            - /planos-acao — planos de ação: busca por título, filtros por status, prioridade e atrasados; a mudança de status é feita nessa tela.
            - /importacoes — envio do CSV de transcrições (ou .txt/.docx de uma reunião); cada lote registra brutos, válidos, incompletos, duplicados e erros, com PDF por lote.
            """;

    /**
     * Guia do agente de suporte técnico. Só descreve o que o sistema realmente faz; nada de
     * dados de clientes — por isso a pergunta de suporte custa ~1.000 tokens.
     */
    static final String GUIA_SUPORTE = MAPA_DE_TELAS + """

            ACESSO
            - Login em /entrar com e-mail e senha. Quem não tem usuário precisa pedir a um administrador.
            - Se a API não responde, o front entra em "modo demonstração": mostra dados de exemplo e avisa no topo da tela. O chat fica desativado nesse modo.

            IMPORTAÇÃO (/importacoes)
            - Arraste o arquivo para a área "Arraste o CSV, .txt ou .docx até aqui" ou clique nela para escolher.
            - Aceita .csv (lote com várias transcrições), .txt ou .docx (uma reunião por arquivo). .doc (Word 97-2003) não é aceito: salve como .docx.
            - Tamanho máximo: 50 MB. Acima disso aparece "O arquivo enviado excede o tamanho máximo permitido.".
            - Campos lidos do CSV: ID_MEETING, DT_MEETING, FORMATO_MEETING, STATUS_MEETING, DURACAO_MEETING, CODT (vendedor), TP_RECURSO, FLG_EXTERNO, ANON_TRANSCRICAO (a transcrição), UF, CNAE, NOME_UNIDADE (cliente), NOME_SEGMENTO, FAIXA_FATURAMENTO_CLIENTE_EC, DT_ULTIMA_PESQUISA, NOTA_NPS.
            - .txt/.docx: o texto é a transcrição. Opcionalmente começa com um cabeçalho entre linhas "---" no formato "Chave: valor" (Cliente, Data, Vendedor, Duração, UF, Segmento, NPS...). Sem cabeçalho, o cliente vem do nome do arquivo e a data também, se houver (ex.: acme_2025-03-10.txt).
            - Cada envio vira um lote com a contagem de brutos, válidos, incompletos, duplicados e erros. Reimportar a mesma reunião cai como duplicada. Cada lote tem o seu PDF.
            - Toda reunião importada é analisada automaticamente: sentimento, risco de churn, scores, dores, produtos, concorrentes, oportunidades e recomendação.

            REUNIÕES (/reunioes e /reunioes/:id)
            - Busca por cliente, vendedor ou identificador; filtros de sentimento, risco de churn, prioridade, completude e período.
            - O detalhe mostra a análise completa, os planos de ação e os comentários, e tem o botão "PDF da reunião".
            - Completude: COMPLETA (80 a 100 pontos), PARCIAL (45 a 79), INCOMPLETA (0 a 44, transcrição quebrada ou sem contexto).

            PLANOS DE AÇÃO (/planos-acao)
            - Quadro com as colunas Pendente, Em andamento, Concluído e Cancelado; busca pelo título.
            - O status muda pelos botões de transição na própria tela. Concluído e Cancelado são finais: o plano não pode mais ser editado.
            - O prazo não pode ser anterior à data de hoje. Plano em aberto com prazo vencido aparece como atrasado.

            RELATÓRIOS EM PDF
            - Executivo: botão "Baixar PDF executivo" no /painel.
            - Por reunião: botão "PDF da reunião" no detalhe.
            - Por lote: link "PDF do lote" em /importacoes.

            CHAT HERMES
            - Agente Reuniões: responde com os dados do banco (reuniões, clientes, planos). Agente Suporte técnico: explica o uso das telas.
            - Limite de 600 caracteres por pergunta e de 8 perguntas por minuto. "Muitas perguntas seguidas" = aguarde 1 minuto.
            - "O Hermes está indisponível no momento" = o provedor de IA não respondeu (cota ou chave); tente de novo em instantes.

            ERROS COMUNS
            - Faixa "modo demonstração": a API não está no ar.
            - "A operação viola uma restrição de integridade": o registro está ligado a outro (ex.: cliente com reuniões vinculadas).
            - "Existem campos inválidos na requisição": algum campo obrigatório ficou vazio ou fora do formato.
            """;

    private final DashboardService dashboardService;
    private final ReuniaoDao reuniaoDao;
    private final ReuniaoRepository reuniaoRepository;
    private final ClienteService clienteService;
    private final ClienteDao clienteDao;
    private final PlanoAcaoDao planoAcaoDao;
    private final ComentarioReuniaoDao comentarioReuniaoDao;

    private volatile String cacheGlobais;
    private volatile long cacheGlobaisEm;

    /**
     * Resultado da montagem.
     *
     * @param texto       bloco que substitui {{CONTEXTO}}
     * @param idsReunioes reuniões presentes no contexto (para o pós-filtro)
     * @param idsPlanos   planos presentes no contexto
     * @param idsClientes clientes presentes no contexto
     * @param titulosPlanos título → id, para identificar fontes citadas pelo nome
     */
    public record Contexto(String texto, Set<Long> idsReunioes, Set<Long> idsPlanos,
                           Set<Long> idsClientes, Map<String, Long> titulosPlanos) {
    }

    /** Contexto do agente de suporte: só o guia, sem consultar o banco. */
    public Contexto montarSuporte() {
        return new Contexto(GUIA_SUPORTE.trim(), Set.of(), Set.of(), Set.of(), Map.of());
    }

    /**
     * @param pergunta  texto do usuário
     * @param reuniaoId reunião aberta na tela (opcional)
     * @param clienteId cliente aberto na tela (opcional)
     */
    public Contexto montar(String pergunta, Long reuniaoId, Long clienteId) {
        Montagem m = new Montagem();
        String n = EscopoGuard.normalizar(pergunta);

        // 1. Sempre: indicadores globais + mapa de telas
        m.secao(indicadoresGlobais());
        m.secao(MAPA_DE_TELAS);

        // 2. Reuniões citadas por ID (ou a reunião aberta na tela)
        Set<Long> ids = new LinkedHashSet<>();
        if (reuniaoId != null) ids.add(reuniaoId);
        ids.addAll(extrairIdsReuniao(pergunta));
        boolean querTranscricao = n.matches(".*\\b(transcri\\w*|trecho|disse|falou|fala|frase)\\b.*");
        ids.stream().limit(MAX_IDS_POR_PERGUNTA).forEach(id -> reuniaoDetalhada(m, id, querTranscricao));

        // 3. Cliente (aberto na tela ou citado pelo nome)
        if (clienteId != null) {
            buscarCliente(clienteId).ifPresent(c -> fichaCliente(m, c));
        } else {
            clientePorNome(m, pergunta);
        }

        // 3b. Planos citados por ID ("plano #17")
        extrairIdsPlano(pergunta).forEach(id -> m.secao(planoAcaoDao.buscarDetalhado(id)
                .map(p -> linhaPlano(p, m) + "\n")
                .orElse("PLANO #" + id + ": não existe no sistema.\n")));

        // 4. Temas
        if (n.matches(".*\\b(churn|risco\\w*|cancel\\w*|perder|perda)\\b.*")) {
            listaReunioes(m, "REUNIÕES COM MAIOR RISCO DE CHURN (top " + TOP + ")",
                    r -> r.riscoChurn() == RiscoChurn.ALTO || r.riscoChurn() == RiscoChurn.MEDIO,
                    Comparator.comparing((IndicadoresReuniao r) -> r.riscoChurn() == RiscoChurn.ALTO ? 0 : 1)
                            .thenComparing(r -> -(r.scoreComercial() == null ? 0 : r.scoreComercial())));
        }
        if (n.matches(".*\\b(oportunidad\\w*|venda\\w*|cross\\w*|upsell|score comercial|melhor\\w*|potencial)\\b.*")) {
            listaReunioes(m, "REUNIÕES COM MAIOR SCORE COMERCIAL (top " + TOP + ")",
                    r -> r.scoreComercial() != null && r.scoreComercial() > 0,
                    Comparator.comparing((IndicadoresReuniao r) -> -r.scoreComercial()));
        }
        if (n.matches(".*\\b(negativ\\w*|critic\\w*|insatisf\\w*|reclama\\w*)\\b.*")) {
            listaReunioes(m, "REUNIÕES COM SENTIMENTO NEGATIVO OU CRÍTICO (mais recentes, top " + TOP + ")",
                    r -> r.sentimento() == Sentimento.NEGATIVO || r.sentimento() == Sentimento.CRITICO,
                    Comparator.comparing((IndicadoresReuniao r) -> -r.id()));
        }
        if (n.matches(".*\\b(atrasad\\w*|atraso\\w*|vencid\\w*|prazo\\w*)\\b.*")) {
            listaPlanos(m, "PLANOS DE AÇÃO ATRASADOS (top " + TOP + ", mais antigos primeiro)",
                    planoAcaoDao.listarAtrasados());
        }
        if (n.matches(".*\\b(pendente\\w*|andamento|abert\\w*)\\b.*")) {
            listaPlanos(m, "PLANOS PENDENTES", pesquisarPlanos(StatusPlanoAcao.PENDENTE));
            listaPlanos(m, "PLANOS EM ANDAMENTO", pesquisarPlanos(StatusPlanoAcao.EM_ANDAMENTO));
        }
        if (n.matches(".*\\b(concluid\\w*|finalizad\\w*|resultado\\w*)\\b.*")) {
            listaPlanos(m, "PLANOS CONCLUÍDOS (prazo mais recente primeiro)", pesquisarPlanosRecentes(StatusPlanoAcao.CONCLUIDO));
        }

        // IDs válidos = os que de fato ficaram no texto (seções cortadas pelo teto não contam)
        String texto = m.texto();
        Set<Long> idsReunioes = idsNoTexto(ROTULO_REUNIAO, texto);
        Set<Long> idsPlanos = idsNoTexto(ROTULO_PLANO, texto);
        Set<Long> idsClientes = idsNoTexto(ROTULO_CLIENTE, texto);
        Map<String, Long> titulos = new LinkedHashMap<>();
        m.titulosPlanos.forEach((titulo, id) -> {
            if (idsPlanos.contains(id)) titulos.put(titulo, id);
        });
        log.debug("Hermes Chat: contexto com {} caracteres, {} reuniões, {} planos",
                texto.length(), idsReunioes.size(), idsPlanos.size());
        return new Contexto(texto, idsReunioes, idsPlanos, idsClientes, titulos);
    }

    // ── 1. Indicadores globais (cache de 60 s: o painel lê a base toda) ─────

    private String indicadoresGlobais() {
        long agora = System.currentTimeMillis();
        String atual = cacheGlobais;
        if (atual != null && agora - cacheGlobaisEm < CACHE_GLOBAIS_MS) return atual;

        DadosDashboard d = dashboardService.getDados();
        Map<StatusPlanoAcao, Long> planos = planoAcaoDao.contarPorStatus();
        Map<StatusCliente, Long> clientes = clienteService.contarPorStatus();

        StringBuilder sb = new StringBuilder("INDICADORES GLOBAIS (dados de agora):\n");
        sb.append("- reuniões analisadas=").append(d.totalReunioes())
          .append(" | clientes citados=").append(d.totalClientes())
          .append(" | churn ALTO=").append(d.totalChurnAlto())
          .append(" | churn MÉDIO=").append(d.totalChurnMedio())
          .append(" | com oportunidade=").append(d.totalOportunidades()).append('\n');
        sb.append("- score médio: qualidade=").append(d.scoreQualidadeMedia())
          .append(" | comercial=").append(d.scoreComercialMedia()).append('\n');
        sb.append("- sentimentos: ").append(mapa(d.sentimentos()))
          .append(" | predominante=").append(d.sentimentoPredominante()).append('\n');
        sb.append("- produtos mais citados: ").append(mapa(d.topProdutos())).append('\n');
        sb.append("- concorrentes mais citados: ").append(mapa(d.topConcorrentes())).append('\n');
        sb.append("- categorias mais frequentes: ").append(mapa(d.topCategorias())).append('\n');
        sb.append("- planos de ação por status: ").append(mapa(planos))
          .append(" | atrasados=").append(planoAcaoDao.contarAtrasados()).append('\n');
        sb.append("- carteira de clientes por status: ").append(mapa(clientes)).append('\n');

        cacheGlobais = sb.toString();
        cacheGlobaisEm = agora;
        return cacheGlobais;
    }

    // ── 2. Reunião detalhada ───────────────────────────────────────────────

    private void reuniaoDetalhada(Montagem m, Long id, boolean incluirTrecho) {
        Optional<Reuniao> encontrada = reuniaoDao.buscarDetalhada(id);
        if (encontrada.isEmpty()) {
            m.secao("REUNIÃO #" + id + ": não existe no sistema.\n");
            return;
        }
        Reuniao r = encontrada.get();
        m.idsReunioes.add(r.getId());

        StringBuilder sb = new StringBuilder();
        sb.append(linhaReuniao(r)).append('\n');
        campo(sb, "tema", r.getTemaReuniao(), 300);
        campo(sb, "resumo", r.getResumoReuniao(), 900);
        campo(sb, "pontos principais", r.getPontosPrincipais(), 600);
        campo(sb, "dores", r.getDoresIdentificadas(), 500);
        campo(sb, "produtos TOTVS citados", r.getProdutosIdentificados(), 300);
        campo(sb, "concorrentes citados", r.getConcorrentesIdentificados(), 300);
        campo(sb, "budget", r.getBudgetIdentificado(), 200);
        campo(sb, "oportunidades", r.getOportunidades(), 500);
        campo(sb, "recomendação final", r.getRecomendacaoFinal(), 500);
        campo(sb, "justificativa do sentimento", r.getSentimentoJustificativa(), 300);
        campo(sb, "motivo de incompletude", r.getMotivoIncompletude(), 200);
        if (incluirTrecho) {
            String fonte = r.getTranscricaoTratada() != null ? r.getTranscricaoTratada() : r.getTranscricaoOriginal();
            campo(sb, "trecho da transcrição", fonte, MAX_TRECHO_TRANSCRICAO);
        }

        List<PlanoAcao> planos = planoAcaoDao.listarPorReuniao(r.getId());
        if (planos.isEmpty()) {
            sb.append("  planos de ação desta reunião: nenhum\n");
        } else {
            sb.append("  planos de ação desta reunião:\n");
            planos.stream().limit(TOP).forEach(p -> sb.append("  ").append(linhaPlano(p, m)).append('\n'));
        }

        List<ComentarioReuniao> comentarios = comentarioReuniaoDao.listarPorReuniao(r.getId(), 0, 5);
        long totalComentarios = comentarioReuniaoDao.contarPorReuniao(r.getId());
        sb.append("  comentários: ").append(totalComentarios).append(totalComentarios > 5 ? " (5 mais recentes abaixo)" : "").append('\n');
        for (ComentarioReuniao c : comentarios) {
            String autor = c.getAutor() != null ? c.getAutor().getNome() : "?";
            sb.append("  - ").append(sanitizar(autor, 80)).append(" em ")
              .append(c.getDataCriacao() != null ? c.getDataCriacao().toLocalDate() : "?")
              .append(": ").append(dado(c.getTexto(), 300)).append('\n');
        }
        m.secao(sb.toString());
    }

    // ── 3. Cliente ─────────────────────────────────────────────────────────

    /**
     * Pelo DAO (Optional), não pelo ClienteService: uma exceção lançada dentro de outro
     * método @Transactional marcaria esta transação como rollback-only.
     */
    private Optional<Cliente> buscarCliente(Long id) {
        return clienteDao.buscarPorId(id);
    }

    private void clientePorNome(Montagem m, String pergunta) {
        for (String termo : candidatosCliente(pergunta)) {
            List<Cliente> cadastrados = clienteDao.pesquisar(termo, null, null, 0, 3);
            if (!cadastrados.isEmpty()) {
                cadastrados.stream().limit(2).forEach(c -> fichaCliente(m, c));
                return;
            }
            // Reuniões cujo cliente não está cadastrado na carteira, só no texto da reunião
            List<Reuniao> soNoTexto = reuniaoRepository.findByClienteContainingIgnoreCase(termo);
            if (!soNoTexto.isEmpty()) {
                StringBuilder sb = new StringBuilder("CLIENTE \"" + sanitizar(termo, 60)
                        + "\" (não cadastrado na carteira; encontrado em " + soNoTexto.size() + " reunião(ões)):\n");
                soNoTexto.stream()
                        .sorted(Comparator.comparing(Reuniao::getData, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(5)
                        .forEach(r -> sb.append("  ").append(linhaReuniaoComPlanosAbertos(r, m)));
                m.secao(sb.toString());
                return;
            }
        }
    }

    private void fichaCliente(Montagem m, Cliente c) {
        m.idsClientes.add(c.getId());
        List<Reuniao> reunioes = reuniaoDao.listarPorCliente(c.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("CLIENTE #").append(c.getId())
          .append(" | nome=").append(sanitizar(c.getNomeExibicao(), 120))
          .append(" | razão social=").append(sanitizar(c.getRazaoSocial(), 120))
          .append(" | CNPJ=").append(nulo(c.getCnpjFormatado()))
          .append(" | segmento=").append(nulo(c.getSegmento()))
          .append(" | status=").append(c.getStatus() != null ? c.getStatus().getDescricao() : "-")
          .append(" | UF=").append(nulo(c.getUf()))
          .append(" | cidade=").append(nulo(c.getCidade()))
          .append(" | faturamento=").append(nulo(c.getFaixaFaturamento()))
          .append(" | NPS=").append(c.getNotaNps() != null ? c.getNotaNps() : "-")
          .append(" | reuniões vinculadas=").append(reunioes.size()).append('\n');
        campo(sb, "observações", c.getObservacoes(), 300);
        if (!reunioes.isEmpty()) {
            sb.append("  últimas reuniões (até 5) e planos em aberto:\n");
            reunioes.stream().limit(5).forEach(r -> sb.append("  ").append(linhaReuniaoComPlanosAbertos(r, m)));
        }
        m.secao(sb.toString());
    }

    private String linhaReuniaoComPlanosAbertos(Reuniao r, Montagem m) {
        m.idsReunioes.add(r.getId());
        StringBuilder sb = new StringBuilder(linhaReuniao(r)).append('\n');
        campo(sb, "resumo", r.getResumoReuniao(), 250);
        planoAcaoDao.listarPorReuniao(r.getId()).stream()
                .filter(p -> p.getStatus() != null && p.getStatus().isEmAberto())
                .limit(5)
                .forEach(p -> sb.append("    ").append(linhaPlano(p, m)).append('\n'));
        return sb.toString();
    }

    // ── 4. Listas por tema ─────────────────────────────────────────────────

    private void listaReunioes(Montagem m, String titulo, Predicate<IndicadoresReuniao> filtro,
                               Comparator<IndicadoresReuniao> ordem) {
        List<Long> ids = reuniaoRepository.findIndicadoresAnalisadas().stream()
                .filter(filtro).sorted(ordem).limit(TOP).map(IndicadoresReuniao::id).toList();
        if (ids.isEmpty()) {
            m.secao(titulo + ": nenhuma.\n");
            return;
        }
        Map<Long, Reuniao> porId = reuniaoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Reuniao::getId, Function.identity()));
        StringBuilder sb = new StringBuilder(titulo).append(":\n");
        ids.stream().map(porId::get).filter(Objects::nonNull).forEach(r -> {
            m.idsReunioes.add(r.getId());
            sb.append("- ").append(linhaReuniao(r)).append('\n');
            campo(sb, "resumo", r.getResumoReuniao(), 200);
        });
        m.secao(sb.toString());
    }

    private void listaPlanos(Montagem m, String titulo, List<PlanoAcao> planos) {
        if (planos.isEmpty()) {
            m.secao(titulo + ": nenhum.\n");
            return;
        }
        StringBuilder sb = new StringBuilder(titulo).append(" — total ").append(planos.size()).append(":\n");
        planos.stream().limit(TOP).forEach(p -> sb.append("- ").append(linhaPlano(p, m)).append('\n'));
        m.secao(sb.toString());
    }

    private List<PlanoAcao> pesquisarPlanos(StatusPlanoAcao status) {
        return planoAcaoDao.pesquisar(new FiltroPlanoAcao(null, null, null, status, null, null, null, "prazo", "asc"), 0, TOP);
    }

    private List<PlanoAcao> pesquisarPlanosRecentes(StatusPlanoAcao status) {
        return planoAcaoDao.pesquisar(new FiltroPlanoAcao(null, null, null, status, null, null, null, "prazo", "desc"), 0, TOP);
    }

    // ── Formatação ─────────────────────────────────────────────────────────

    static String linhaReuniao(Reuniao r) {
        return "REUNIÃO #" + r.getId()
                + " | data=" + nulo(r.getData())
                + " | cliente=" + sanitizar(r.getCliente(), 120)
                + " | vendedor=" + sanitizar(r.getVendedor(), 80)
                + " | categoria=" + nulo(r.getCategoriaReuniao())
                + " | sentimento=" + nulo(r.getSentimento())
                + " | churn=" + nulo(r.getRiscoChurn())
                + " | score comercial=" + nulo(r.getScoreComercial())
                + " | score qualidade=" + nulo(r.getScoreQualidade())
                + " | prioridade=" + nulo(r.getPrioridade())
                + " | completude=" + nulo(r.getStatusCompletude())
                + (r.getNotaNps() != null ? " | NPS=" + r.getNotaNps() : "");
    }

    private String linhaPlano(PlanoAcao p, Montagem m) {
        m.idsPlanos.add(p.getId());
        if (p.getTitulo() != null) m.titulosPlanos.put(p.getTitulo(), p.getId());
        return linhaPlano(p, LocalDate.now());
    }

    static String linhaPlano(PlanoAcao p, LocalDate hoje) {
        StringBuilder sb = new StringBuilder("PLANO #").append(p.getId())
                .append(" | título=").append(sanitizar(p.getTitulo(), 150))
                .append(" | reunião=#").append(p.getReuniao() != null ? p.getReuniao().getId() : "?")
                .append(" | responsável=").append(p.getResponsavel() != null ? sanitizar(p.getResponsavel().getNome(), 80) : "-")
                .append(" | prioridade=").append(p.getPrioridade() != null ? p.getPrioridade().name() : "-")
                .append(" | status=").append(p.getStatus() != null ? p.getStatus().getDescricao() : "-")
                .append(" | prazo=").append(nulo(p.getPrazo()));
        if (p.getPrazo() != null && p.getStatus() != null && p.getStatus().isEmAberto() && p.getPrazo().isBefore(hoje)) {
            sb.append(" | ATRASADO há ").append(ChronoUnit.DAYS.between(p.getPrazo(), hoje)).append(" dia(s)");
        }
        if (p.getResultado() != null && !p.getResultado().isBlank()) {
            sb.append(" | resultado=").append(dado(p.getResultado(), 200));
        }
        return sb.toString();
    }

    private static void campo(StringBuilder sb, String rotulo, String valor, int max) {
        if (valor == null || valor.isBlank()) return;
        sb.append("  ").append(rotulo).append(": ").append(dado(valor, max)).append('\n');
    }

    /** Texto vindo de transcrição/análise/comentário: sempre como DADO, nunca instrução. */
    static String dado(String texto, int max) {
        return "<dado>" + sanitizar(texto, max) + "</dado>";
    }

    /**
     * Remove {@code <} e {@code >} (impede fechar {@code <dado>} ou abrir tags falsas),
     * chaves duplas, caracteres de controle e quebras em excesso; corta no limite.
     */
    static String sanitizar(String texto, int max) {
        if (texto == null) return "-";
        String limpo = texto
                .replaceAll("[<>]", " ")
                .replace("{{", "{ {").replace("}}", "} }")
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ")
                .replaceAll("\\n{2,}", "\n")
                .replaceAll("[ \\t]{2,}", " ")
                .replace("\n", " / ")
                .trim();
        return limpo.length() <= max ? limpo : limpo.substring(0, max).trim() + "…";
    }

    /** IDs de reunião citados como "#482", "reunião 482" ou "reunião nº 482". */
    static List<Long> extrairIdsReuniao(String pergunta) {
        if (pergunta == null) return new ArrayList<>();
        // "plano #17" é plano, não reunião
        return extrairIds(PADRAO_ID, PADRAO_ID_PLANO.matcher(pergunta).replaceAll(" "));
    }

    /** IDs de plano citados como "plano #17" ou "plano de ação 17". */
    static List<Long> extrairIdsPlano(String pergunta) {
        if (pergunta == null) return new ArrayList<>();
        return extrairIds(PADRAO_ID_PLANO, pergunta);
    }

    private static List<Long> extrairIds(Pattern padrao, String texto) {
        List<Long> ids = new ArrayList<>();
        Matcher matcher = padrao.matcher(texto);
        while (matcher.find() && ids.size() < MAX_IDS_POR_PERGUNTA) {
            try {
                Long id = Long.parseLong(matcher.group(1));
                if (!ids.contains(id)) ids.add(id);
            } catch (NumberFormatException ignorada) {
                // número grande demais: não é ID
            }
        }
        return ids;
    }

    /**
     * Possíveis nomes de cliente na pergunta, do mais longo para o mais curto.
     * Ex.: "planos do cliente Acme Ltda estão atrasados?" → ["Acme Ltda", "Acme"].
     */
    static List<String> candidatosCliente(String pergunta) {
        List<String> candidatos = new ArrayList<>();
        if (pergunta == null) return candidatos;
        Matcher matcher = PADRAO_CLIENTE.matcher(pergunta);
        while (matcher.find()) {
            List<String> palavras = new ArrayList<>();
            for (String p : matcher.group(1).trim().split("\\s+")) {
                String limpa = p.replaceAll("[?!,;:]+$", "");
                if (PALAVRAS_PARADA.contains(limpa.toLowerCase())) break;
                palavras.add(limpa);
                if (!limpa.equals(p)) break; // pontuação encerra o nome
            }
            for (int tamanho = palavras.size(); tamanho >= 1; tamanho--) {
                String termo = String.join(" ", palavras.subList(0, tamanho)).trim();
                if (termo.length() >= 3 && !candidatos.contains(termo)) candidatos.add(termo);
            }
        }
        return candidatos;
    }

    static Set<Long> idsNoTexto(Pattern rotulo, String texto) {
        Set<Long> ids = new LinkedHashSet<>();
        Matcher matcher = rotulo.matcher(texto);
        while (matcher.find()) {
            try {
                ids.add(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException ignorada) {
                // não acontece com \\d+ de até 9 dígitos, mas não derruba o chat
            }
        }
        return ids;
    }

    private static String mapa(Map<?, Long> mapa) {
        if (mapa == null || mapa.isEmpty()) return "-";
        return mapa.entrySet().stream()
                .map(e -> sanitizar(String.valueOf(e.getKey()), 60) + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String nulo(Object valor) {
        return valor == null ? "-" : sanitizar(String.valueOf(valor), 120);
    }

    /** Acumula seções respeitando o teto de caracteres. */
    static final class Montagem {
        private final StringBuilder sb = new StringBuilder();
        final Set<Long> idsReunioes = new LinkedHashSet<>();
        final Set<Long> idsPlanos = new LinkedHashSet<>();
        final Set<Long> idsClientes = new LinkedHashSet<>();
        final Map<String, Long> titulosPlanos = new LinkedHashMap<>();
        private boolean cortado = false;

        void secao(String texto) {
            if (cortado || texto == null || texto.isBlank()) return;
            int restante = MAX_CARACTERES_CONTEXTO - sb.length();
            if (texto.length() + 1 > restante) {
                // Uma seção cortada pela metade pode citar algo sem os dados — melhor omitir.
                sb.append("[contexto limitado: há mais registros no sistema; peça ao usuário para refinar a pergunta]\n");
                cortado = true;
                return;
            }
            sb.append(texto);
            if (!texto.endsWith("\n")) sb.append('\n');
            sb.append('\n');
        }

        String texto() {
            return sb.toString().trim();
        }
    }
}
