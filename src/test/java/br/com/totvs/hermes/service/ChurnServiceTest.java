package br.com.totvs.hermes.service;

import br.com.totvs.hermes.model.Insight;
import br.com.totvs.hermes.model.Reuniao;

import java.util.ArrayList;
import java.util.List;

/**
 * Testes manuais do cálculo de risco de churn.
 * Padrão: apenas try-catch, sem JUnit/AssertJ/Mockito.
 * ChurnService não tem dependências — basta instanciar.
 */
public class ChurnServiceTest {

    private static int passou = 0, falhou = 0;

    public static void main(String[] args) {
        verificar("Teste 1 — cálculo de rescisão na folha não é churn",
                "aqui eu executo o cálculo de férias e o cálculo de rescisão do colaborador", "BAIXO");
        verificar("Teste 2 — cancelamento de lançamentos não é churn",
                "cálculo de 13º, diferença de 13º, cancelamento, lançamentos", "BAIXO");
        verificar("Teste 3 — exemplo de fala em demonstração não é churn",
                "o usuário pode dizer: não quero mais esse motorista nessa viagem", "BAIXO");
        verificar("Teste 4 — processo de cancelamento do contrato é churn alto",
                "na verdade a gente tá em processo de cancelamento do contrato, tá?", "ALTO");
        verificar("Teste 5 — não renovação é churn alto",
                "a diretoria já decidiu, não vamos renovar em julho", "ALTO");
        verificar("Teste 6 — mandato de pessoa que não vai renovar não é churn",
                "a gestão dele termina em julho e ele já disse que não vai renovar", "BAIXO");
        verificar("Teste 7 — insatisfação continua sendo churn médio",
                "estamos insatisfeito com o suporte de vocês", "MEDIO");

        System.out.println("\nResultado: " + passou + " passaram, " + falhou + " falharam.");
        if (falhou > 0) System.exit(1);
    }

    private static void verificar(String nome, String texto, String esperado) {
        try {
            List<Insight> insights = new ArrayList<>();
            String resultado = new ChurnService()
                    .calcular(texto.toLowerCase(), List.of(), texto, insights, new Reuniao());
            if (!esperado.equals(resultado))
                throw new AssertionError("Esperava " + esperado + ", mas retornou " + resultado);
            passou++;
            System.out.println("[OK] " + nome);
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] " + nome + " — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] " + nome + " — exceção inesperada: " + e.getMessage());
        }
    }
}
