package br.com.totvs.insight360.model;

/**
 * Testes manuais para a entidade Reuniao.
 * Padrão: apenas try-catch, sem JUnit/AssertJ/Mockito.
 * Reuniao é um POJO — instanciado com new, sem mock.
 */
public class ReuniaoTest {

    public static void main(String[] args) {
        int passou = 0, falhou = 0;

        // Teste 1 — isCompleta() retorna true para status COMPLETA
        try {
            Reuniao r = new Reuniao();
            r.setStatusCompletude(StatusCompletude.COMPLETA);

            if (!r.isCompleta()) throw new AssertionError("Esperava isCompleta()=true para COMPLETA, mas foi false");
            passou++;
            System.out.println("[OK] Teste 1 — isCompleta() retorna true para COMPLETA");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 1 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 1 — exceção inesperada: " + e.getMessage());
        }

        // Teste 2 — isParcial() retorna true para status PARCIAL
        try {
            Reuniao r = new Reuniao();
            r.setStatusCompletude(StatusCompletude.PARCIAL);

            if (!r.isParcial()) throw new AssertionError("Esperava isParcial()=true para PARCIAL, mas foi false");
            if (r.isCompleta()) throw new AssertionError("isCompleta() deveria ser false quando status é PARCIAL");
            passou++;
            System.out.println("[OK] Teste 2 — isParcial() retorna true para PARCIAL");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 2 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 2 — exceção inesperada: " + e.getMessage());
        }

        // Teste 3 — isIncompleta() retorna true para status INCOMPLETA
        try {
            Reuniao r = new Reuniao();
            r.setStatusCompletude(StatusCompletude.INCOMPLETA);

            if (!r.isIncompleta()) throw new AssertionError("Esperava isIncompleta()=true para INCOMPLETA, mas foi false");
            if (r.isCompleta()) throw new AssertionError("isCompleta() deveria ser false quando status é INCOMPLETA");
            if (r.isParcial())  throw new AssertionError("isParcial() deveria ser false quando status é INCOMPLETA");
            passou++;
            System.out.println("[OK] Teste 3 — isIncompleta() retorna true para INCOMPLETA");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 3 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 3 — exceção inesperada: " + e.getMessage());
        }

        // Teste 4 — getSentimentoFormatado() retorna "Crítico" para CRITICO
        try {
            Reuniao r = new Reuniao();
            r.setSentimento(Sentimento.CRITICO);

            String resultado = r.getSentimentoFormatado();

            if (!"Crítico".equals(resultado))
                throw new AssertionError("Esperava 'Crítico', mas obteve: '" + resultado + "'");
            passou++;
            System.out.println("[OK] Teste 4 — getSentimentoFormatado() retorna 'Crítico' para CRITICO");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 4 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 4 — exceção inesperada: " + e.getMessage());
        }

        // Teste 5 — getSentimentoFormatado() retorna "Neutro" para sentimento null
        try {
            Reuniao r = new Reuniao();
            r.setSentimento(null);

            String resultado = r.getSentimentoFormatado();

            if (!"Neutro".equals(resultado))
                throw new AssertionError("Esperava 'Neutro' para sentimento null, mas obteve: '" + resultado + "'");
            passou++;
            System.out.println("[OK] Teste 5 — getSentimentoFormatado() retorna 'Neutro' para null");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 5 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 5 — exceção inesperada: " + e.getMessage());
        }

        // Teste 6 — getDuracaoDisplay() retorna "1h 30min" para 90 minutos
        try {
            Reuniao r = new Reuniao();
            r.setDuracaoMinutos(90);
            r.setDuracaoFormatada(null);

            String resultado = r.getDuracaoDisplay();

            if (!"1h 30min".equals(resultado))
                throw new AssertionError("Esperava '1h 30min' para 90 minutos, mas obteve: '" + resultado + "'");
            passou++;
            System.out.println("[OK] Teste 6 — getDuracaoDisplay() retorna '1h 30min' para 90 minutos");
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
