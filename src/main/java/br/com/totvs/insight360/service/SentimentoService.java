package br.com.totvs.insight360.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável pelo cálculo de sentimento da transcrição.
 */
@Service
@RequiredArgsConstructor
public class SentimentoService {

    private static final String[] POS = {
            "ótimo", "gostei muito", "excelente", "muito bom", "resolveu",
            "melhorou bastante", "estamos satisfeitos", "ótima apresentação",
            "muito interessante", "faz todo sentido", "adorei", "funciona muito bem",
            "boa experiência", "muito satisfeito", "bacana", "maravilhoso"
    };
    private static final String[] NEG = {
            "problema sério", "sofrendo muito", "ruim demais", "muito difícil",
            "retrabalho constante", "erro grave", "demora absurda", "insatisfeito",
            "péssimo", "não funciona", "falha constante", "impossível usar",
            "atraso grande", "frustrado", "decepcionante", "não atende"
    };

    /**
     * Calcula o sentimento com base no texto e no contexto da análise.
     *
     * @return ‘String’[2] onde [0] é o nome do sentimento e [1] é a justificativa
     */
    public String[] calcular(String lower, List<String> dores, List<String> concorrentes,
                              String churn, List<String> oportunidades) {
        int p = 0, n = 0;
        List<String> posFound = new ArrayList<>(), negFound = new ArrayList<>();
        for (String s : POS) if (lower.contains(s)) { p++; posFound.add(s); }
        for (String s : NEG) if (lower.contains(s)) { n++; negFound.add(s); }
        n += dores.size() / 2;

        String sent, just;

        if ("ALTO".equals(churn)) {
            sent = "CRITICO";
            just = "Risco de cancelamento detectado na transcrição."
                    + (negFound.isEmpty() ? "" : " Expressões negativas: "
                    + String.join(", ", negFound.subList(0, Math.min(3, negFound.size()))) + ".");
        } else if (!concorrentes.isEmpty() && n > p) {
            sent = "CRITICO";
            just = "Concorrente mencionado (" + String.join(", ", concorrentes)
                    + ") com tom predominantemente negativo. Alto risco comercial.";
        } else if (!oportunidades.isEmpty() && p >= n) {
            sent = "OPORTUNIDADE_COMERCIAL";
            just = "Oportunidades de cross-selling identificadas com tom positivo. "
                    + oportunidades.get(0) + ".";
        } else if (p > n * 2) {
            sent = "POSITIVO";
            just = "Predominância de expressões positivas: "
                    + String.join(", ", posFound.subList(0, Math.min(3, posFound.size()))) + ".";
        } else if (n > p * 2) {
            sent = "NEGATIVO";
            just = "Predominância de expressões negativas: "
                    + String.join(", ", negFound.subList(0, Math.min(3, negFound.size()))) + "."
                    + (dores.isEmpty() ? "" : " " + dores.size() + " dores identificadas.");
        } else if (p > 0 && n > 0) {
            sent = "MISTO";
            just = "Expressões positivas (" + p + ") e negativas (" + n
                    + ") coexistem — sentimento ambivalente.";
        } else if (n > 0 || !dores.isEmpty()) {
            sent = "NEGATIVO";
            just = "Presença de dores e expressões negativas sem contraposição positiva relevante.";
        } else {
            sent = "NEUTRO";
            just = "Sem expressões emocionais claras — reunião predominantemente informativa ou técnica.";
        }
        return new String[]{sent, just};
    }
}
