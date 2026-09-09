package br.com.totvs.insight360.dto.resposta;

/**
 * Detalhe de um campo que falhou na validação de entrada.
 *
 * @param campo    nome do atributo rejeitado
 * @param mensagem motivo da rejeição
 */
public record ErroCampo(String campo, String mensagem) {
}
