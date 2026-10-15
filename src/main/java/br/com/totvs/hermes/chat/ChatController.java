package br.com.totvs.hermes.chat;

import br.com.totvs.hermes.dto.requisicao.ChatRequisicao;
import br.com.totvs.hermes.dto.resposta.ChatResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hermes Chat — assistente que responde só sobre os dados deste sistema.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Hermes Chat", description = "Assistente conversacional sobre reuniões, clientes e planos de ação")
public class ChatController {

    private final HermesChatService chatService;

    @PostMapping
    @Operation(summary = "Envia uma pergunta ao Hermes",
            description = "Responde apenas com base nos dados do sistema. 429 = limite de perguntas por minuto; "
                    + "503 = nenhum provedor de IA disponível.")
    public ResponseEntity<ChatResposta> perguntar(@RequestBody @Valid ChatRequisicao requisicao,
                                                  HttpServletRequest http) {
        return ResponseEntity.ok(chatService.responder(requisicao, ipDe(http)));
    }

    /**
     * No Render (e atrás de qualquer proxy) o IP real vem no X-Forwarded-For.
     * Usa o primeiro da lista; sem o cabeçalho, o endereço da conexão.
     */
    static String ipDe(HttpServletRequest http) {
        String encaminhado = http.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
