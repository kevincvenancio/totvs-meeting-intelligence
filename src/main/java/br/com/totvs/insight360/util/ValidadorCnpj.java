package br.com.totvs.insight360.util;

/**
 * Validação de CNPJ pelos dígitos verificadores (módulo 11).
 * <p>
 * Utilitário estático, no mesmo estilo de {@link TextoUtils}: sem estado e sem
 * dependência de framework, para poder ser usado em qualquer camada.
 */
public final class ValidadorCnpj {

    private static final int TAMANHO_CNPJ = 14;
    private static final int[] PESOS_PRIMEIRO_DIGITO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO_DIGITO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private ValidadorCnpj() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    /** Remove máscara e espaços, deixando apenas os dígitos. */
    public static String normalizar(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("\\D", "");
    }

    /**
     * Valida um CNPJ com ou sem máscara.
     *
     * @return {@code true} quando os 14 dígitos e os verificadores estão corretos
     */
    public static boolean isValido(String cnpj) {
        String digitos = normalizar(cnpj);
        if (digitos == null || digitos.length() != TAMANHO_CNPJ || todosDigitosIguais(digitos)) {
            return false;
        }
        int primeiroDigito = calcularDigito(digitos, PESOS_PRIMEIRO_DIGITO);
        int segundoDigito = calcularDigito(digitos, PESOS_SEGUNDO_DIGITO);
        return primeiroDigito == Character.getNumericValue(digitos.charAt(12))
                && segundoDigito == Character.getNumericValue(digitos.charAt(13));
    }

    private static boolean todosDigitosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

    private static int calcularDigito(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
