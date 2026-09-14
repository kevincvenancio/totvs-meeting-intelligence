package br.com.totvs.hermes.dto.resposta;

import java.time.LocalDate;

/**
 * Referência mínima a uma reunião, usada dentro das respostas de plano de ação
 * e de comentário para evitar payloads desnecessariamente grandes.
 */
public record ReuniaoReferenciaResposta(
        Long id,
        String idExterno,
        LocalDate data,
        String cliente
) {
}
