package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.util.TextoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável pela geração de objetos Insight a partir de uma Reuniao.
 */
@Service
@RequiredArgsConstructor
public class InsightExtratorService {

    /**
     * Gera insights adicionais de oportunidades de venda cruzada para a reunião.
     *
     * @return lista de oportunidades identificadas (labels)
     */
    public List<String> extrairOportunidadesVendaCruzada(String lower,
                                                          List<Insight> intuicoes, Reuniao r) {
        List<String> ops = new ArrayList<>();

        if (TextoUtils.contemQualquer(lower, "folha de pagamento", "departamento pessoal", "holerite"))
            ops.add("Venda cruzada: TOTVS RM / Folha de Pagamento");
        if (TextoUtils.contemQualquer(lower, "relatório gerencial", "dashboard", "business intelligence", "indicador de desempenho"))
            ops.add("Venda cruzada: TOTVS BI / Analytics");
        if (TextoUtils.contemQualquer(lower, "integração entre sistemas", "api de integração"))
            ops.add("Venda cruzada: TOTVS Fluig / Integração");
        if (TextoUtils.contemQualquer(lower, "gestão comercial", "funil de vendas", "pipeline de vendas", "leads"))
            ops.add("Venda cruzada: TOTVS CRM");
        if (TextoUtils.contemQualquer(lower, "fluxo de caixa", "contas a pagar", "contas a receber", "cobrança"))
            ops.add("Venda cruzada: TOTVS Gestão Financeira / Techfin");
        if (TextoUtils.contemQualquer(lower, "inteligência artificial", "machine learning", "plataforma de dados"))
            ops.add("Venda cruzada: TOTVS Carol (IA e Dados)");
        if (TextoUtils.contemQualquer(lower, "ponto eletrônico", "escala de trabalho", "banco de horas", "jornada de trabalho"))
            ops.add("Venda cruzada: TOTVS Agora (Ponto e Escalas)");

        for (String op : ops) {
            adicionarInsight(intuicoes, r,
                    "Oportunidade de venda cruzada",
                    op,
                    "ALTA",
                    "Identificado com base nas dores e contexto da reunião",
                    80);
        }
        return ops;
    }

    /**
     * Cria e adiciona um Insight à lista fornecida.
     */
    public void adicionarInsight(List<Insight> intuicoes, Reuniao r, String tipo,
                                  String desc, String prio, String trecho, int conf) {
        Insight ins = new Insight();
        ins.setReuniao(r);
        ins.setTipo(tipo);
        ins.setDescricao(desc);
        ins.setPrioridade(prio);
        ins.setTrechoOrigem(trecho != null && !trecho.isBlank() ? trecho : "Menção encontrada na transcrição");
        ins.setConfianca(conf);
        intuicoes.add(ins);
    }
}
