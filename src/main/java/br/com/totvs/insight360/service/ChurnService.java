package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.util.TextoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço responsável pelo cálculo de risco de churn.
 */
@Service
@RequiredArgsConstructor
public class ChurnService {

    static final String[] CHURN_ALTO = {
            "cancelar contrato", "não renovar", "vamos sair", "cancelamento",
            "rescisão", "trocar de fornecedor", "sair da totvs",
            "migrar para outro", "não quero mais", "fim do contrato"
    };
    static final String[] CHURN_MEDIO = {
            "avaliando outra solução", "insatisfeito com", "não está atendendo",
            "procurar alternativa", "proposta do concorrente",
            "estamos olhando para", "pensando em trocar"
    };

    /**
     * Calcula o risco de churn e adiciona insights relevantes.
     *
     * @return "ALTO", "MEDIO" ou "BAIXO"
     */
    public String calcular(String lower, List<String> concorrentes,
                           String texto, List<Insight> intuicoes, Reuniao r) {
        for (String kw : CHURN_ALTO) {
            if (lower.contains(kw)) {
                adicionarInsight(intuicoes, r,
                        "Risco de churn — ALTO",
                        "Sinal forte de possível cancelamento detectado",
                        TextoUtils.extrairTrecho(texto, kw, 100),
                        88);
                return "ALTO";
            }
        }
        for (String kw : CHURN_MEDIO) {
            if (lower.contains(kw)) {
                adicionarInsight(intuicoes, r,
                        "Risco de churn — MÉDIO",
                        "Sinais de insatisfação ou avaliação de alternativas",
                        TextoUtils.extrairTrecho(texto, kw, 100),
                        75);
                return "MEDIO";
            }
        }
        if (!concorrentes.isEmpty()) return "MEDIO";
        return "BAIXO";
    }

    private void adicionarInsight(List<Insight> intuicoes, Reuniao r, String tipo,
                                  String desc, String trecho, int conf) {
        Insight ins = new Insight();
        ins.setReuniao(r);
        ins.setTipo(tipo);
        ins.setDescricao(desc);
        ins.setPrioridade("ALTA");
        ins.setTrechoOrigem(trecho != null && !trecho.isBlank() ? trecho : "Menção encontrada na transcrição");
        ins.setConfianca(conf);
        intuicoes.add(ins);
    }
}