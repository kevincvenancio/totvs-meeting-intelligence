package br.com.totvs.hermes.service;

import br.com.totvs.hermes.model.Insight;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.util.TextoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço responsável pelo cálculo de risco de churn.
 */
@Service
@RequiredArgsConstructor
public class ChurnService {

    /**
     * Expressões sobre o contrato com o fornecedor. Palavras soltas como "cancelamento",
     * "rescisão" ou "não quero mais" aparecem o tempo todo em demonstrações (cálculo de
     * rescisão na folha, cancelamento de nota fiscal) e marcavam churn falso.
     */
    static final String[] CHURN_ALTO = {
            "cancelar o contrato", "cancelar contrato", "cancelar nosso contrato",
            "cancelamento do contrato", "pedido de cancelamento", "rescindir o contrato",
            "encerrar o contrato", "rescisão do contrato com",
            "não vamos renovar", "não renovar o contrato", "não vou renovar",
            "sair da totvs", "trocar de fornecedor",
            "migrar para outro fornecedor", "migrar para outro sistema", "migrar para outro erp"
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