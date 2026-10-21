package br.com.totvs.hermes.ia;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe o pipeline de IA (Hugging Face) pela API principal, para analisar uma
 * transcrição nova sem precisar importá-la. A análise das reuniões importadas
 * usa a mesma IA automaticamente (ver {@code AnaliseTranscricaoService}).
 */
@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
@Tag(name = "IA Hermes", description = "Análise de transcrições com modelos do Hugging Face")
public class IaController {

    private final HermesIaClient ia;
    private final FilaAnaliseIa fila;

    public record AnaliseIaRequisicao(
            @NotBlank @Size(min = 20, max = 400_000, message = "A transcrição precisa ter entre 20 e 400.000 caracteres")
            String transcricao,
            @Pattern(regexp = "hibrido|modelo|heuristico", message = "Modo deve ser hibrido, modelo ou heuristico")
            String modo) {
    }

    public record StatusIaResposta(boolean habilitada, boolean online, String modo, String url) {
    }

    @GetMapping("/status")
    @Operation(summary = "Situação do serviço de IA", description = "habilitada = HERMES_IA_URL configurado; online = respondeu agora")
    public StatusIaResposta status() {
        return new StatusIaResposta(ia.habilitado(), ia.online(), ia.modo(), ia.habilitado() ? ia.url() : null);
    }

    @GetMapping("/fila")
    @Operation(summary = "Andamento da análise da base pelo modelo",
            description = "Reuniões na fila, processadas, falhas, segundos por reunião e minutos restantes.")
    public FilaAnaliseIa.StatusFila fila() {
        return fila.status();
    }

    @PostMapping("/fila")
    @Operation(summary = "Manda a base de reuniões para o modelo",
            description = "todas=false (padrão): só as reuniões que ainda não passaram pela IA. "
                    + "todas=true: reprocessa a base inteira. Roda em segundo plano; acompanhe em GET /fila.")
    public FilaAnaliseIa.StatusFila analisarBase(@RequestParam(defaultValue = "false") boolean todas) {
        fila.enfileirarBase(todas);
        return fila.status();
    }

    @PostMapping("/analises")
    @Operation(summary = "Analisa uma transcrição nova com a IA",
            description = "Transcrição → Processamento → Modelo → Indicadores → Recomendação. "
                    + "Devolve produto, sentimento, risco de churn, oportunidade, termos e recomendações. "
                    + "503 = serviço de IA desligado ou fora do ar.")
    public ResponseEntity<JsonNode> analisar(@RequestBody @Valid AnaliseIaRequisicao requisicao) {
        return ResponseEntity.ok(ia.analisar(requisicao.transcricao(), requisicao.modo()));
    }
}
