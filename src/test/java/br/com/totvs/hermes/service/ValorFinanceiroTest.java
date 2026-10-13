package br.com.totvs.hermes.service;

import java.util.Objects;

/**
 * Testes manuais da regra que decide se um número citado na reunião é orçamento.
 * Padrão: apenas try-catch, sem JUnit/AssertJ/Mockito.
 * encontrarValorFinanceiro é estático — nenhum mock necessário.
 */
public class ValorFinanceiroTest {

    private static int passou = 0, falhou = 0;

    public static void main(String[] args) {
        verificar("Teste 1 — quantidade de colaboradores não é orçamento",
                "daqui a um ano tu tá ali com 3 mil colaboradores. aí demitiu 500", null);
        verificar("Teste 2 — notas e clientes não são orçamento",
                "a gente emite 40 mil notas por mês e tem 5 mil clientes ativos", null);
        verificar("Teste 3 — vidas e quilômetros não são orçamento",
                "são 12 mil vidas no plano e 300 km de rota", null);
        verificar("Teste 4 — valor com R$ e ordem de grandeza",
                "o projeto fica em R$ 15 mil por mês", "R$ 15 mil");
        verificar("Teste 5 — valor com R$ e centavos",
                "a mensalidade é R$ 2.500,00.", "R$ 2.500,00");
        verificar("Teste 6 — valor seguido de reais",
                "a gente gasta uns 50 mil reais com isso", "50 mil reais");
        verificar("Teste 7 — palavra financeira logo antes do número",
                "o orçamento aprovado é de 200 mil para o ano", "200 mil");
        verificar("Teste 8 — milhões não é truncado em mil",
                "investimento de R$ 3 milhões", "R$ 3 milhões");
        verificar("Teste 9 — pula a quantidade e pega o dinheiro que vem depois",
                "temos 3 mil colaboradores e o custo atual é de R$ 80 mil por ano", "R$ 80 mil");

        System.out.println("\nResultado: " + passou + " passaram, " + falhou + " falharam.");
        if (falhou > 0) System.exit(1);
    }

    private static void verificar(String nome, String texto, String esperado) {
        try {
            String resultado = AnaliseTranscricaoService.encontrarValorFinanceiro(texto);
            if (!Objects.equals(esperado, resultado))
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
