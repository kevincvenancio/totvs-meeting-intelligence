package br.com.totvs.hermes.dto.requisicao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pergunta enviada ao Hermes Chat.
 *
 * @param mensagem  pergunta do usuário
 * @param historico turnos anteriores (só {@code user}/{@code assistant}; o servidor descarta o resto)
 * @param reuniaoId reunião aberta na tela, quando houver — foca o contexto nela
 * @param clienteId cliente aberto na tela, quando houver
 * @param agente    {@code REUNIOES} (padrão: consulta o banco) ou {@code SUPORTE} (como usar o sistema)
 * @param tela      rota em que o usuário está (ex.: {@code /importacoes}) — ajuda o agente de suporte
 */
public record ChatRequisicao(
        @NotBlank(message = "A mensagem é obrigatória.")
        @Size(max = 600, message = "A mensagem pode ter no máximo 600 caracteres.")
        String mensagem,

        @Size(max = 20, message = "O histórico pode ter no máximo 20 mensagens.")
        List<@Valid MensagemHistorico> historico,

        Long reuniaoId,
        Long clienteId,

        @Pattern(regexp = "REUNIOES|SUPORTE", message = "O agente deve ser REUNIOES ou SUPORTE.")
        String agente,

        // Vai para o prompt do agente de suporte: só aceita formato de rota, nunca texto livre
        @Size(max = 80, message = "A rota da tela pode ter no máximo 80 caracteres.")
        @Pattern(regexp = "/[a-z0-9/-]*", message = "A tela deve ser uma rota do sistema, ex.: /importacoes.")
        String tela
) {

    /** Agente efetivo: sem valor, é o de reuniões. */
    public boolean suporte() {
        return "SUPORTE".equals(agente);
    }

    /**
     * @param papel {@code user} ou {@code assistant}
     * @param texto conteúdo da mensagem
     */
    public record MensagemHistorico(
            String papel,
            @Size(max = 2000, message = "Cada mensagem do histórico pode ter no máximo 2000 caracteres.")
            String texto
    ) {
    }
}
