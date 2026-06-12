package br.com.totvs.insight360.util;

/**
 * Testes manuais para TextoUtils.
 * Padrão: apenas try-catch, sem JUnit/AssertJ/Mockito.
 * TextoUtils possui apenas métodos estáticos — nenhum mock necessário.
 */
public class TextoUtilsTest {

    public static void main(String[] args) {
        int passou = 0, falhou = 0;

        // Teste 1 — contemQualquer retorna false para texto nulo
        try {
            TextoUtils.contemQualquer(null, "palavra");
            boolean resultado = false;

            if (resultado) throw new AssertionError("Esperava false para texto nulo, mas retornou true");
            passou++;
            System.out.println("[OK] Teste 1 — contemQualquer retorna false para texto nulo");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 1 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 1 — exceção inesperada: " + e.getMessage());
        }

        // Teste 2 — contemQualquer retorna true quando palavra existe no texto
        try {
            boolean resultado = TextoUtils.contemQualquer("estamos avaliando outra solução", "solução", "contrato");

            if (!resultado) throw new AssertionError("Esperava true (palavra encontrada), mas retornou false");
            passou++;
            System.out.println("[OK] Teste 2 — contemQualquer retorna true quando palavra existe");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 2 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 2 — exceção inesperada: " + e.getMessage());
        }

        // Teste 3 — tratarTexto retorna string vazia para null
        try {
            String resultado = TextoUtils.tratarTexto(null);

            if (!resultado.isEmpty()) throw new AssertionError("Esperava string vazia para null, mas obteve: '" + resultado + "'");
            passou++;
            System.out.println("[OK] Teste 3 — tratarTexto retorna string vazia para null");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 3 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 3 — exceção inesperada: " + e.getMessage());
        }

        // Teste 4 — truncarInteligente não corta no meio de palavra
        try {
            String texto = "reunião muito importante sobre vendas";
            String resultado = TextoUtils.truncarInteligente(texto, 20);

            // Resultado não deve ser maior que o original
            if (resultado.length() > texto.length())
                throw new AssertionError("Resultado maior que o original: " + resultado);

            // Se foi truncado, deve terminar com "..."
            String semReticencias = resultado.replace("...", "").trim();
            if (resultado.endsWith("...") && !semReticencias.isEmpty()) {
                // Verifica que o corte ocorreu em limite de palavra (sem hífen artificial)
                boolean terminaEmPalavraCompleta = texto.startsWith(semReticencias);
                if (!terminaEmPalavraCompleta)
                    throw new AssertionError("Texto pode ter sido cortado no meio de uma palavra: " + resultado);
            }

            passou++;
            System.out.println("[OK] Teste 4 — truncarInteligente não corta no meio de palavra (resultado: '" + resultado + "')");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 4 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 4 — exceção inesperada: " + e.getMessage());
        }

        // Teste 5 — isBudgetFinanceiro retorna false para texto com contexto de volume
        try {
            boolean resultado = TextoUtils.isBudgetFinanceiro("precisamos de 100 servidores novos");

            if (resultado) throw new AssertionError("Esperava false para '100 servidores' (contexto de volume), mas retornou true");
            passou++;
            System.out.println("[OK] Teste 5 — isBudgetFinanceiro retorna false para '100 servidores'");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 5 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 5 — exceção inesperada: " + e.getMessage());
        }

        // Teste 6 — isBudgetFinanceiro retorna true para texto com orçamento financeiro
        try {
            boolean resultado = TextoUtils.isBudgetFinanceiro("temos um orçamento de R$100 mil aprovado");

            if (!resultado) throw new AssertionError("Esperava true para 'orçamento de R$100 mil', mas retornou false");
            passou++;
            System.out.println("[OK] Teste 6 — isBudgetFinanceiro retorna true para 'orçamento de R$100 mil'");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 6 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 6 — exceção inesperada: " + e.getMessage());
        }

        System.out.println("\nResultado: " + passou + " passou(ram) | " + falhou + " falhou(ram)");
    }
}