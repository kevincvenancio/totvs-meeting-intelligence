package br.com.totvs.insight360.migracao;

/**
 * Metadados de uma coluna da tabela de destino, lidos do catálogo do Oracle.
 *
 * @param nome    nome da coluna em maiúsculas
 * @param tipo    código {@link java.sql.Types} da coluna
 * @param tamanho tamanho declarado (0 quando não se aplica)
 */
public record ColunaDestino(String nome, int tipo, int tamanho) {
}
