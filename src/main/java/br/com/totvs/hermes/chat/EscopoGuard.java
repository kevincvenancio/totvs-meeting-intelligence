package br.com.totvs.hermes.chat;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guardas de escopo do Hermes — a parte da defesa que não depende do modelo.
 * <ul>
 *   <li><b>Pré-filtro</b>: roda antes do LLM, para não gastar cota com pergunta
 *       fora do produto ou tentativa de injeção.</li>
 *   <li><b>Pós-filtro</b>: valida a resposta (vazamento de prompt/modelo, IDs de
 *       reunião que não estavam no contexto, tamanho).</li>
 * </ul>
 * Sem dependências: dá para testar instanciando direto.
 */
@Component
public class EscopoGuard {

    public static final int MAX_CARACTERES_RESPOSTA = 1500;
    static final String AVISO_NAO_CONFIRMADO = "(Alguns dados não puderam ser confirmados.)";

    /** Continuação de conversa só passa sem palavra do domínio se for curta ("e a segunda?"). */
    private static final int MAX_CARACTERES_CONTINUACAO = 160;

    // ── Padrões (aplicados sobre o texto normalizado: minúsculo e sem acento) ──

    private static final List<Pattern> INJECAO = compilar(
            "ignor\\w*\\s+(as\\s+|todas\\s+as\\s+|suas\\s+|tuas\\s+|essas\\s+|minhas\\s+|o\\s+)?(\\w+\\s+)?(instruc|regra|orientac|prompt)",
            "esquec\\w*\\s+(as\\s+|todas\\s+as\\s+|suas\\s+)?(\\w+\\s+)?(regra|instruc)",
            "desconsider\\w*\\s+(as\\s+|suas\\s+)?(regra|instruc)",
            "system\\s*prompt",
            "prompt\\s+de\\s+sistema",
            "voce\\s+agora\\s+e",
            "a\\s+partir\\s+de\\s+agora\\s+voce",
            "finja\\s+(ser|que)",
            "aja\\s+como",
            "\\bdan\\b",
            "jailbreak",
            "modo\\s+(desenvolvedor|developer|deus)",
            "revel\\w*\\s+(suas\\s+|as\\s+|tuas\\s+)?(instruc|regra)",
            "ignore\\s+(all|previous|the)\\s+",
            "responda\\s+livremente");

    private static final List<Pattern> IDENTIDADE = compilar(
            "(seu|teu)\\s+prompt",
            "(qual|que)\\s+(e\\s+o\\s+)?(seu\\s+|teu\\s+)?(modelo|llm|provedor)",
            "(qual|que)\\s+ia\\s+(voce|e)",
            "quem\\s+(te\\s+)?(criou|treinou|desenvolveu)",
            "voce\\s+e\\s+(o\\s+)?(chatgpt|gpt|llama|claude|gemini)");

    private static final Pattern ALTERACAO = Pattern.compile(
            "^(por\\s+favor\\s*,?\\s*)?(apag|exclu|delet|remov|cri[ae]|cadastr|alter|edit|atualiz|mud[ae]|conclu|cancel|finaliz|marq|reabr|import[ae]\\s)\\w*");

    /** Fora do produto mesmo no meio de uma conversa válida. */
    private static final List<Pattern> FORA_DO_ESCOPO = compilar(
            "\\bpoem", "\\bpoesia", "\\bpiada", "\\btraduz", "\\btraducao", "\\btranslate",
            "capital\\s+d[aoe]s?\\b", "\\bpython\\b", "\\bjavascript\\b", "\\btypescript\\b",
            "\\bcodigo\\s+(em|de|para)\\b", "\\bfutebol", "\\bpolitic", "\\beleic", "\\bpresidente\\s+d[oa]\\b",
            "\\bhoroscopo", "previsao\\s+do\\s+tempo", "\\bletra\\s+d[ae]\\b", "\\breceita\\s+de\\s+(bolo|torta|pao)");

    private static final Pattern SAUDACAO_OU_AJUDA = Pattern.compile(
            "^(oi|ola|ola hermes|oi hermes|bom dia|boa tarde|boa noite|e ai|hello|hi|obrigad\\w*|valeu|ajuda|help)[!.?, ]*$"
                    + "|o\\s+que\\s+voce\\s+(faz|pode)|como\\s+voce\\s+(pode\\s+)?(me\\s+)?ajuda");

    private static final Set<String> DOMINIO_EXATO = Set.of(
            "rm", "dor", "dores", "nps", "csv", "pdf", "totvs", "hermes", "plano", "planos", "acao", "acoes",
            "kpi", "kpis", "upsell", "lote", "lotes", "importar", "importo", "importei", "importou", "importe");

    private static final List<String> DOMINIO_PREFIXO = List.of(
            "reuni", "client", "carteira", "praz", "atrasad", "vencid", "churn", "risco", "riscos",
            "sentiment", "score", "concorr", "produt", "budget", "orcament", "oportunidad", "comentari",
            "painel", "dashboard", "importac", "importad", "relatori", "transcri", "prioridad", "protheus", "datasul",
            "fluig", "vendedor", "venda", "segment", "responsav", "pendent", "andament", "concluid",
            "cancelad", "indicador", "recomendac", "categori", "completude", "insight", "feedback",
            "cross-sell", "crosssell");

    /** Vocabulário extra aceito só pelo agente de suporte técnico (uso das telas). */
    private static final Set<String> SUPORTE_EXATO = Set.of("txt", "docx", "doc", "login", "menu", "upload", "kanban");
    private static final List<String> SUPORTE_PREFIXO = List.of(
            "tela", "botao", "filtr", "entrar", "senha", "erro", "export", "baix", "arquivo", "formato",
            "coluna", "demonstra", "vitrine", "status", "quadro", "acess", "naveg", "pesquis", "busca",
            "cadastr", "usuario", "perfil", "analis", "duplicad", "cabecalho", "limite", "indisponivel");

    private static final Pattern ID_NA_PERGUNTA = Pattern.compile(
            "#\\s*\\d+|reunia?o?e?s?\\s*(n|no|num|numero)?\\.?\\s*\\d+");

    private static final List<String> VAZAMENTO = List.of(
            "regras inegociaveis", "sua unica funcao", "<contexto>", "</contexto>", "{{contexto}}",
            "<guia>", "</guia>", "{{tela}}", "modo suporte tecnico do sistema",
            "llama", "openrouter", "groq", "nousresearch", "nous research", "cerebras", "ollama");
    private static final Pattern VAZAMENTO_NOUS = Pattern.compile("\\bnous\\b");

    private static final Pattern CITACAO_REUNIAO = Pattern.compile(
            "(?iu)reuni[aã]o\\s*(?:n[º°o]\\.?\\s*)?#\\s*(\\d{1,9})");
    private static final Pattern CITACAO_SOLTA = Pattern.compile("(?<![\\p{L}\\d])#(\\d+)");

    // ── Tipos de resultado ─────────────────────────────────────────────

    public enum Veredito { PERMITIDA, INJECAO, IDENTIDADE, ALTERACAO, AJUDA, FORA_DO_ESCOPO }

    /**
     * @param veredito     classificação da pergunta
     * @param respostaFixa texto a devolver sem chamar o LLM (null quando permitida)
     */
    public record Avaliacao(Veredito veredito, String respostaFixa) {
        public boolean bloqueada() {
            return veredito != Veredito.PERMITIDA;
        }

        /** Só recusa de verdade conta como "fora do escopo" para o front. */
        public boolean foraDoEscopo() {
            return veredito == Veredito.INJECAO || veredito == Veredito.FORA_DO_ESCOPO;
        }
    }

    /**
     * @param texto      resposta final, já filtrada
     * @param substituida true quando a resposta do modelo foi trocada pela recusa
     */
    public record RespostaFiltrada(String texto, boolean substituida) {
    }

    // ── Pré-filtro ─────────────────────────────────────────────────────

    /**
     * Classifica a pergunta antes de qualquer chamada ao LLM.
     *
     * @param pergunta     texto do usuário (já validado quanto a tamanho)
     * @param continuacao  existe turno anterior do usuário dentro do escopo
     */
    public Avaliacao avaliar(String pergunta, boolean continuacao) {
        return avaliar(pergunta, continuacao, false);
    }

    /**
     * @param suporte true para o agente de suporte técnico, que aceita também o vocabulário de
     *                uso das telas ("tela", "erro", "botão"...)
     */
    public Avaliacao avaliar(String pergunta, boolean continuacao, boolean suporte) {
        String n = normalizar(pergunta);
        if (n.isBlank()) return recusa(Veredito.FORA_DO_ESCOPO);

        if (algum(INJECAO, n)) return recusa(Veredito.INJECAO);
        if (algum(IDENTIDADE, n)) return new Avaliacao(Veredito.IDENTIDADE, PromptHermes.IDENTIDADE);

        boolean dominio = temDominio(n, suporte);

        if (dominio && ALTERACAO.matcher(n).find()) {
            return new Avaliacao(Veredito.ALTERACAO,
                    "Eu só consulto e explico os dados — não crio, altero nem apago nada. "
                            + "Para isso, use a tela " + telaSugerida(n) + ".");
        }

        if (algum(FORA_DO_ESCOPO, n)) return recusa(Veredito.FORA_DO_ESCOPO);
        if (dominio) return new Avaliacao(Veredito.PERMITIDA, null);

        if (SAUDACAO_OU_AJUDA.matcher(n).find()) {
            return new Avaliacao(Veredito.AJUDA,
                    "Oi, sou o Hermes. Posso te ajudar com reuniões, clientes e planos de ação. "
                            + "Experimente: \"Quais clientes têm risco de churn alto?\" ou \"Resuma a reunião #482\".");
        }

        if (continuacao && n.length() <= MAX_CARACTERES_CONTINUACAO) {
            return new Avaliacao(Veredito.PERMITIDA, null);
        }
        return recusa(Veredito.FORA_DO_ESCOPO);
    }

    /** A pergunta contém alguma palavra do domínio do produto (ou um #id)? */
    public boolean temDominio(String texto) {
        return temDominio(texto, false);
    }

    public boolean temDominio(String texto, boolean suporte) {
        String n = normalizar(texto);
        if (ID_NA_PERGUNTA.matcher(n).find()) return true;
        for (String palavra : n.split("[^a-z0-9-]+")) {
            if (palavra.isEmpty()) continue;
            if (DOMINIO_EXATO.contains(palavra)) return true;
            if (suporte && SUPORTE_EXATO.contains(palavra)) return true;
            for (String prefixo : DOMINIO_PREFIXO) {
                if (palavra.startsWith(prefixo)) return true;
            }
            if (suporte) {
                for (String prefixo : SUPORTE_PREFIXO) {
                    if (palavra.startsWith(prefixo)) return true;
                }
            }
        }
        return false;
    }

    // ── Pós-filtro ─────────────────────────────────────────────────────

    /**
     * Valida a resposta do modelo.
     *
     * @param resposta         texto devolvido pelo LLM
     * @param idsReunioes      reuniões que estavam no contexto enviado
     * @param idsPlanos        planos de ação que estavam no contexto enviado
     */
    public RespostaFiltrada filtrarResposta(String resposta, Set<Long> idsReunioes, Set<Long> idsPlanos) {
        if (resposta == null || resposta.isBlank()) {
            return new RespostaFiltrada(PromptHermes.NAO_ENCONTRADO, false);
        }

        String n = normalizar(resposta);
        if (VAZAMENTO.stream().anyMatch(n::contains) || VAZAMENTO_NOUS.matcher(n).find()) {
            return new RespostaFiltrada(PromptHermes.RECUSA, true);
        }

        boolean removeuCitacao = false;

        // "Reunião #N" com N fora do contexto → vira "uma reunião"
        StringBuilder sb = new StringBuilder();
        Matcher m = CITACAO_REUNIAO.matcher(resposta);
        while (m.find()) {
            Long id = paraLong(m.group(1));
            if (id != null && idsReunioes.contains(id)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            } else {
                m.appendReplacement(sb, "uma reunião");
                removeuCitacao = true;
            }
        }
        m.appendTail(sb);

        // "#N" solto (listas) precisa ser reunião ou plano conhecido
        String texto = sb.toString();
        sb = new StringBuilder();
        m = CITACAO_SOLTA.matcher(texto);
        while (m.find()) {
            Long id = paraLong(m.group(1));
            boolean conhecido = id != null && (idsReunioes.contains(id) || idsPlanos.contains(id));
            if (conhecido) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            } else {
                m.appendReplacement(sb, "");
                removeuCitacao = true;
            }
        }
        m.appendTail(sb);
        texto = sb.toString().replaceAll("[ \\t]{2,}", " ").trim();

        if (texto.length() > MAX_CARACTERES_RESPOSTA) {
            int corte = texto.lastIndexOf(' ', MAX_CARACTERES_RESPOSTA - 1);
            texto = texto.substring(0, corte > 0 ? corte : MAX_CARACTERES_RESPOSTA - 1).trim() + "…";
        }
        if (removeuCitacao) {
            texto = texto + "\n\n" + AVISO_NAO_CONFIRMADO;
        }
        return new RespostaFiltrada(texto, false);
    }

    // ── Apoio ──────────────────────────────────────────────────────────

    /** Minúsculo, sem acento, espaços colapsados. */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('º', 'o').replace('°', 'o');
        return semAcento.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static String telaSugerida(String n) {
        if (n.contains("plano") || n.contains("acao") || n.contains("acoes")) return "Planos de ação (/planos-acao)";
        if (n.contains("comentari")) return "de detalhe da reunião (/reunioes/:id)";
        if (n.contains("client")) return "Clientes (/clientes)";
        if (n.contains("import") || n.contains("csv")) return "Importações (/importacoes)";
        return "Reuniões (/reunioes)";
    }

    private static Avaliacao recusa(Veredito veredito) {
        return new Avaliacao(veredito, PromptHermes.RECUSA);
    }

    private static boolean algum(List<Pattern> padroes, String texto) {
        return padroes.stream().anyMatch(p -> p.matcher(texto).find());
    }

    private static List<Pattern> compilar(String... regex) {
        return java.util.Arrays.stream(regex).map(Pattern::compile).toList();
    }

    private static Long paraLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
