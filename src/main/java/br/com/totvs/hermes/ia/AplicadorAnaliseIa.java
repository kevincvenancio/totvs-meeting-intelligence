package br.com.totvs.hermes.ia;

import br.com.totvs.hermes.model.Insight;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.model.RiscoChurn;
import br.com.totvs.hermes.model.Sentimento;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Grava na {@link Reuniao} o resultado do serviço de IA e devolve os insights que
 * justificam cada indicador (trecho da transcrição + probabilidade do modelo).
 * <p>
 * A IA tem a palavra final sobre churn, sentimento, prioridade, produtos e recomendação.
 * Os demais campos (completude, dores, categorias, scores de qualidade) continuam
 * vindo das regras do {@code AnaliseTranscricaoService}.
 */
public final class AplicadorAnaliseIa {

    public static final String ORIGEM_REGRAS = "REGRAS";
    /** Analisada pelas regras e aguardando a fila de IA. */
    public static final String ORIGEM_PENDENTE = "IA_PENDENTE";
    public static final String TIPO_CHURN = "Risco de churn (IA)";
    public static final String TIPO_OPORTUNIDADE = "Oportunidade comercial (IA)";
    public static final String TIPO_PRODUTO = "Produto identificado (IA)";
    public static final String TIPO_RECOMENDACAO = "Recomendação (IA)";

    /** Limites das colunas VARCHAR2 (ver docs/ddl-oracle.sql). */
    private static final int MAX_PRODUTOS = 255;
    private static final int MAX_TERMOS = 500;
    private static final int MAX_BUDGET = 255;

    private AplicadorAnaliseIa() {
    }

    /**
     * Aplica a análise da IA na reunião.
     *
     * @return insights novos (ainda não salvos) com as evidências da IA
     */
    public static List<Insight> aplicar(Reuniao reuniao, JsonNode ia) {
        JsonNode churn = ia.path("risco_churn");
        JsonNode oport = ia.path("oportunidade_comercial");
        JsonNode sent = ia.path("sentimento");
        JsonNode indicadores = ia.path("indicadores");

        RiscoChurn risco = enumOuNulo(RiscoChurn.class, churn.path("nivel").asText(""));
        if (risco != null) {
            reuniao.setRiscoChurn(risco);
        }
        Sentimento sentimento = enumOuNulo(Sentimento.class, indicadores.path("sentimento_hermes").asText(""));
        if (sentimento != null) {
            reuniao.setSentimento(sentimento);
            reuniao.setSentimentoJustificativa("IA (Hugging Face): " + sent.path("justificativa").asText("")
                    + " Score " + String.format(java.util.Locale.ROOT, "%+.2f", sent.path("score").asDouble()) + ".");
        }
        String prioridade = indicadores.path("prioridade").asText("");
        if (!prioridade.isBlank()) {
            reuniao.setPrioridade(prioridade);
        }

        List<String> produtos = new ArrayList<>();
        for (JsonNode p : ia.path("produtos_identificados")) {
            produtos.add(p.path("produto").asText());
        }
        if (!produtos.isEmpty()) {
            reuniao.setProdutosIdentificados(cortar(String.join(", ", produtos), MAX_PRODUTOS));
        }

        String budgetIa = oport.path("budget").asText("");
        String budgetAtual = reuniao.getBudgetIdentificado();
        if (!budgetIa.isBlank() && (budgetAtual == null || budgetAtual.isBlank() || "Não identificado".equals(budgetAtual))) {
            reuniao.setBudgetIdentificado(cortar(budgetIa, MAX_BUDGET));
        }

        List<String> termos = new ArrayList<>();
        for (JsonNode t : ia.path("termos_principais")) {
            termos.add(t.path("termo").asText());
        }

        StringBuilder rec = new StringBuilder();
        for (JsonNode a : ia.path("recomendacoes")) {
            if (rec.length() > 0) {
                rec.append('\n');
            }
            rec.append('[').append(a.path("prioridade").asText()).append("] ")
                    .append(a.path("acao").asText())
                    .append(" (").append(a.path("prazo").asText()).append(", ")
                    .append(a.path("responsavel").asText()).append(") — ")
                    .append(a.path("motivo").asText());
        }
        if (rec.length() > 0) {
            reuniao.setRecomendacaoFinal(rec.toString());
        }

        reuniao.setScoreChurn(inteiroOuNulo(churn.path("score")));
        reuniao.setScoreOportunidade(inteiroOuNulo(oport.path("score")));
        reuniao.setSaudeCliente(inteiroOuNulo(indicadores.path("saude_cliente")));
        reuniao.setTermosPrincipais(termos.isEmpty() ? null : cortar(String.join(", ", termos), MAX_TERMOS));
        reuniao.setOrigemAnalise("IA_" + ia.path("modo").asText("hibrido").toUpperCase(java.util.Locale.ROOT));
        reuniao.setAnaliseIa(ia.toString());

        return insights(reuniao, ia);
    }

    /** Limpa os campos de IA quando a reunião é (re)analisada só pelas regras. */
    public static void limpar(Reuniao reuniao) {
        reuniao.setScoreChurn(null);
        reuniao.setScoreOportunidade(null);
        reuniao.setSaudeCliente(null);
        reuniao.setTermosPrincipais(null);
        reuniao.setAnaliseIa(null);
        reuniao.setOrigemAnalise(ORIGEM_REGRAS);
    }

    /** Reunião analisada pelas regras que vai para a fila de IA. */
    public static void marcarPendente(Reuniao reuniao) {
        limpar(reuniao);
        reuniao.setOrigemAnalise(ORIGEM_PENDENTE);
    }

    private static List<Insight> insights(Reuniao reuniao, JsonNode ia) {
        List<Insight> lista = new ArrayList<>();
        JsonNode churn = ia.path("risco_churn");
        JsonNode oport = ia.path("oportunidade_comercial");

        if (!"BAIXO".equals(churn.path("nivel").asText())) {
            for (JsonNode e : churn.path("evidencias")) {
                lista.add(insight(reuniao, TIPO_CHURN,
                        "Risco de churn " + churn.path("nivel").asText() + " (" + churn.path("score").asInt() + "/100) — "
                                + e.path("origem").asText(),
                        "ALTO".equals(churn.path("nivel").asText()) ? "ALTA" : "MEDIA",
                        e.path("trecho").asText(), confianca(e)));
            }
        }
        if (!"BAIXA".equals(oport.path("nivel").asText())) {
            for (JsonNode e : oport.path("evidencias")) {
                lista.add(insight(reuniao, TIPO_OPORTUNIDADE,
                        "Oportunidade " + oport.path("nivel").asText() + " (" + oport.path("score").asInt() + "/100) — "
                                + e.path("origem").asText(),
                        "ALTA".equals(oport.path("nivel").asText()) ? "ALTA" : "MEDIA",
                        e.path("trecho").asText(), confianca(e)));
            }
        }
        for (JsonNode p : ia.path("produtos_identificados")) {
            if ("inferido pelo modelo".equals(p.path("origem").asText())) {
                lista.add(insight(reuniao, TIPO_PRODUTO,
                        p.path("produto").asText() + " inferido pela descrição da necessidade (sem citar o nome)",
                        "MEDIA", p.path("evidencia").asText(),
                        (int) Math.round(p.path("confianca").asDouble() * 100)));
            }
        }
        JsonNode primeira = ia.path("recomendacoes").path(0);
        if (!primeira.isMissingNode()) {
            lista.add(insight(reuniao, TIPO_RECOMENDACAO, primeira.path("acao").asText(),
                    primeira.path("prioridade").asText("MEDIA"), primeira.path("motivo").asText(), 80));
        }
        return lista;
    }

    private static Insight insight(Reuniao r, String tipo, String descricao, String prioridade,
                                   String trecho, int confianca) {
        Insight ins = new Insight();
        ins.setReuniao(r);
        ins.setTipo(tipo);
        ins.setDescricao(descricao);
        ins.setPrioridade(prioridade);
        ins.setTrechoOrigem(trecho == null || trecho.isBlank() ? "Análise da IA sobre a transcrição" : trecho);
        ins.setConfianca(Math.max(0, Math.min(100, confianca)));
        return ins;
    }

    /** Probabilidade do modelo (0-1) em porcentagem; evidência de regra explícita vale 90. */
    private static int confianca(JsonNode evidencia) {
        JsonNode p = evidencia.path("probabilidade");
        return p.isNumber() ? (int) Math.round(p.asDouble() * 100) : 90;
    }

    private static Integer inteiroOuNulo(JsonNode no) {
        return no.isNumber() ? no.asInt() : null;
    }

    private static <E extends Enum<E>> E enumOuNulo(Class<E> tipo, String valor) {
        try {
            return valor == null || valor.isBlank() ? null : Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String cortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
