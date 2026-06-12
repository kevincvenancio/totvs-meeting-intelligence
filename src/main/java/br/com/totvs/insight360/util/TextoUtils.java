package br.com.totvs.insight360.util;

import java.text.Normalizer;


public class TextoUtils {

    private static final String[] CONTEXTO_FINANCEIRO = {
            "orcamento", "verba", "investimento", "custo", "preco",
            "proposta", "contrato", "aprovacao", "roi", "payback",
            "valor do projeto", "budget", "mensalidade", "licenca",
            "implantacao", "pagamento", "fatura"
    };

    private static final String[] CONTEXTO_VOLUME = {
            "servidores", "usuarios", "registros", "notas", "produtos",
            "pessoas", "funcionarios", "linhas", "transacoes",
            "pedidos", "documentos", "arquivos", "maquinas",
            "equipamentos", "unidades", "lojas", "filiais"
    };

    public static final String CAT_DEMO         = "Demonstracao de Produto";
    public static final String CAT_NEGOCIACAO   = "Negociacao Comercial";
    public static final String CAT_DIAGNOSTICO  = "Diagnostico / Discovery";
    public static final String CAT_SUPORTE      = "Suporte / Problema";
    public static final String CAT_TECNICA      = "Reuniao Tecnica";
    public static final String CAT_IMPLANTACAO  = "Implantacao / Onboarding";
    public static final String CAT_FOLLOWUP     = "Follow-up Comercial";
    public static final String CAT_INSUFICIENTE = "Dados Insuficientes";

    public static final String SENT_CRITICO      = "Critico";
    public static final String SENT_NEGATIVO     = "Negativo";
    public static final String SENT_OPORTUNIDADE = "Oportunidade Comercial";
    public static final String SENT_POSITIVO     = "Positivo";
    public static final String SENT_NEUTRO       = "Neutro";
    public static final String SENT_MISTO        = "Misto";

    /** Tratamento completo sem correcao de encoding. */
    public static String tratarTexto(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String t = texto
                .replace("[PESSOA]",  "Contato")
                .replace("[EMPRESA]", "Cliente")
                .replace("[LOCAL]",   "Local")
                .replace("[PRODUTO]", "Produto");
        t = Normalizer.normalize(t, Normalizer.Form.NFC);
        t = t.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        t = t.replace("\r\n", "\n").replace("\r", "\n");
        t = t.replaceAll("[ \\t]+", " ");
        t = t.replaceAll("\n{3,}", "\n\n");
        return t.trim();
    }

    public static int contarPalavras(String texto) {
        if (texto == null || texto.isBlank()) return 0;
        return texto.trim().split("\\s+").length;
    }

    public static boolean contemQualquer(String texto, String... palavras) {
        if (texto == null) return false;
        String lower = texto.toLowerCase();
        for (String p : palavras) { if (lower.contains(p.toLowerCase())) return true; }
        return false;
    }

    public static String extrairTrecho(String texto, String palavraChave, int contexto) {
        if (texto == null || palavraChave == null) return "";
        int idx = texto.toLowerCase().indexOf(palavraChave.toLowerCase());
        if (idx == -1) return "";
        int start = Math.max(0, idx - contexto);
        int end   = Math.min(texto.length(), idx + palavraChave.length() + contexto);
        String trecho = texto.substring(start, end).trim()
                .replace("\n", " ").replaceAll("\\s+", " ");
        return "..." + trecho + "...";
    }

    /** Trunca texto sem cortar palavras no meio. */
    public static String truncarInteligente(String texto, int max) {
        if (texto == null || texto.isBlank()) return "-";
        String t = texto.trim();
        if (t.length() <= max) return t;
        int corte = t.lastIndexOf(' ', max);
        if (corte > (int)(max * 0.6)) return t.substring(0, corte) + "...";
        return t.substring(0, max) + "...";
    }

    /** Formata categoria para exibicao padronizada. */
    public static String formatarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return CAT_INSUFICIENTE;
        String u = categoria.toUpperCase().trim();
        if (u.contains("DEMO") || u.contains("DEMONSTR"))   return CAT_DEMO;
        if (u.contains("NEGOCI"))                            return CAT_NEGOCIACAO;
        if (u.contains("DIAGN") || u.contains("DISCOVERY")) return CAT_DIAGNOSTICO;
        if (u.contains("SUPORTE") || u.contains("PROBLEMA")) return CAT_SUPORTE;
        if (u.contains("IMPLANT") || u.contains("ONBOARD")) return CAT_IMPLANTACAO;
        if (u.contains("FOLLOW") || u.contains("ACOMPAN"))  return CAT_FOLLOWUP;
        if (u.contains("TECNIC"))                            return CAT_TECNICA;
        return categoria;
    }

    /** Formata sentimento para exibicao padronizada. */
    public static String formatarSentimento(String sentimento) {
        if (sentimento == null || sentimento.isBlank()) return SENT_NEUTRO;
        return switch (sentimento.toUpperCase().trim()) {
            case "CRITICO"                                -> SENT_CRITICO;
            case "NEGATIVO"                               -> SENT_NEGATIVO;
            case "OPORTUNIDADE_COMERCIAL", "OPORTUNIDADE" -> SENT_OPORTUNIDADE;
            case "POSITIVO"                               -> SENT_POSITIVO;
            case "MISTO"                                  -> SENT_MISTO;
            default                                       -> SENT_NEUTRO;
        };
    }

    /**
     * REGRA CRITICA: so retorna true se houver contexto financeiro claro.
     * "100 mil servidores" NAO e budget. "orcamento de R$100 mil" SIM.
     */
    public static boolean isBudgetFinanceiro(String trecho) {
        if (trecho == null || trecho.isBlank()) return false;
        // Normaliza: remove acentos e converte para minusculo
        String lower = Normalizer.normalize(trecho, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
        for (String kw : CONTEXTO_VOLUME)     { if (lower.contains(kw)) return false; }
        for (String kw : CONTEXTO_FINANCEIRO) { if (lower.contains(kw)) return true;  }
        return false;
    }

    public static String formatarPercentual(long valor, long total) {
        if (total <= 0) return "0%";
        return (100 * valor / total) + "%";
    }

    public static int scoreSeguro(Integer score) {
        if (score == null) return 0;
        return Math.max(0, Math.min(100, score));
    }
}
// ── Compatibilidade CLI ───────────────────────────────────────────


