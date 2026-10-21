package br.com.totvs.hermes.ia;

import br.com.totvs.hermes.model.Insight;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.model.RiscoChurn;
import br.com.totvs.hermes.model.Sentimento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Testes do {@link AplicadorAnaliseIa} com uma resposta no formato exato do serviço
 * Python ({@code ciencia-de-dados/api.py}, POST /analisar).
 * <p>
 * Padrão do projeto: main() com try/catch, sem framework.
 */
public class AplicadorAnaliseIaTest {

    /** Resposta real do pipeline (modo híbrido) para um caso de churn implícito, resumida. */
    static final String RESPOSTA_IA = """
            {
              "versao": "1.0.0", "modo": "hibrido",
              "produtos_identificados": [
                {"produto": "TOTVS RH / Folha", "confianca": 0.934, "origem": "inferido pelo modelo",
                 "evidencia": "Em outubro a folha saiu com erro para trezentos funcionários"}
              ],
              "produto_principal": "TOTVS RH / Folha",
              "concorrentes": [],
              "sentimento": {"rotulo": "NEGATIVO", "score": -0.61, "distribuicao_trechos": {"positivo": 0.0, "neutro": 0.25, "negativo": 0.75},
                             "justificativa": "Modelo: -0.61 (média ponderada de 4 trechos)"},
              "risco_churn": {"nivel": "ALTO", "score": 88, "score_modelo": 97, "score_lexico": 10, "sinais": [],
                              "evidencias": [{"trecho": "quanto custaria a multa para encerrar o relacionamento com vocês",
                                              "probabilidade": 0.93, "origem": "modelo"}]},
              "oportunidade_comercial": {"nivel": "BAIXA", "score": 12, "score_modelo": 8, "score_lexico": 5,
                                         "budget": null, "venda_cruzada": [], "evidencias": []},
              "dores": [],
              "termos_principais": [{"termo": "folha pagamento", "relevancia": 5.0}, {"termo": "multa", "relevancia": 2.0}],
              "indicadores": {"saude_cliente": 14, "prioridade": "Alta", "sentimento_hermes": "CRITICO"},
              "recomendacoes": [{"acao": "Acionar Customer Success e o gerente da conta com um plano de retenção",
                                 "prioridade": "ALTA", "prazo": "24 horas", "responsavel": "Customer Success",
                                 "motivo": "Risco de churn 88/100."}],
              "tempo_ms": 2140
            }
            """;

    public static void main(String[] args) {
        int ok = 0;
        int falhas = 0;
        ObjectMapper json = new ObjectMapper();

        try {
            JsonNode ia = json.readTree(RESPOSTA_IA);
            Reuniao r = new Reuniao();
            r.setRiscoChurn(RiscoChurn.BAIXO);          // o que as regras tinham concluído
            r.setSentimento(Sentimento.NEUTRO);
            r.setBudgetIdentificado("Não identificado");

            List<Insight> insights = AplicadorAnaliseIa.aplicar(r, ia);

            verificar(r.getRiscoChurn() == RiscoChurn.ALTO, "IA sobrepõe o churn das regras");
            verificar(r.getSentimento() == Sentimento.CRITICO, "sentimento usa o enum do Java");
            verificar("Alta".equals(r.getPrioridade()), "prioridade");
            verificar(Integer.valueOf(88).equals(r.getScoreChurn()), "score de churn");
            verificar(Integer.valueOf(14).equals(r.getSaudeCliente()), "saúde do cliente");
            verificar("TOTVS RH / Folha".equals(r.getProdutosIdentificados()), "produto inferido");
            verificar("folha pagamento, multa".equals(r.getTermosPrincipais()), "termos principais");
            verificar("IA_HIBRIDO".equals(r.getOrigemAnalise()), "origem da análise");
            verificar("Não identificado".equals(r.getBudgetIdentificado()), "budget nulo da IA não apaga o campo");
            verificar(r.getRecomendacaoFinal().startsWith("[ALTA] Acionar Customer Success"), "recomendação");
            verificar(r.getAnaliseIa() != null && r.getAnaliseIa().contains("risco_churn"), "JSON completo gravado");

            Insight churn = insights.stream().filter(i -> AplicadorAnaliseIa.TIPO_CHURN.equals(i.getTipo())).findFirst().orElseThrow();
            verificar(churn.getConfianca() == 93, "confiança = probabilidade do modelo");
            verificar(churn.getTrechoOrigem().contains("multa"), "evidência é o trecho da transcrição");
            verificar(insights.stream().anyMatch(i -> AplicadorAnaliseIa.TIPO_PRODUTO.equals(i.getTipo())), "insight de produto inferido");
            verificar(insights.stream().noneMatch(i -> AplicadorAnaliseIa.TIPO_OPORTUNIDADE.equals(i.getTipo())), "oportunidade BAIXA não vira insight");
            System.out.println("OK  aplicar resposta completa da IA");
            ok++;
        } catch (Exception | AssertionError e) {
            System.out.println("FALHOU aplicar: " + e.getMessage());
            falhas++;
        }

        try {
            Reuniao r = new Reuniao();
            r.setScoreChurn(50);
            r.setAnaliseIa("{}");
            AplicadorAnaliseIa.limpar(r);
            verificar(r.getScoreChurn() == null && r.getAnaliseIa() == null, "campos de IA limpos");
            verificar(AplicadorAnaliseIa.ORIGEM_REGRAS.equals(r.getOrigemAnalise()), "origem REGRAS");
            System.out.println("OK  limpar quando a IA não respondeu");
            ok++;
        } catch (Exception | AssertionError e) {
            System.out.println("FALHOU limpar: " + e.getMessage());
            falhas++;
        }

        try {
            Reuniao r = new Reuniao();
            r.setRiscoChurn(RiscoChurn.MEDIO);
            AplicadorAnaliseIa.aplicar(r, json.readTree("{\"risco_churn\": {\"nivel\": \"DESCONHECIDO\"}}"));
            verificar(r.getRiscoChurn() == RiscoChurn.MEDIO, "valor desconhecido não apaga o das regras");
            System.out.println("OK  resposta parcial ou com valor inesperado");
            ok++;
        } catch (Exception | AssertionError e) {
            System.out.println("FALHOU resposta parcial: " + e.getMessage());
            falhas++;
        }

        System.out.println("\n" + ok + " ok, " + falhas + " falha(s)");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    private static void verificar(boolean condicao, String descricao) {
        if (!condicao) {
            throw new AssertionError(descricao);
        }
    }
}
