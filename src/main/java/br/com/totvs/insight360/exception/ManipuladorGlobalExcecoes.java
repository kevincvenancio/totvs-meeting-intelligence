package br.com.totvs.insight360.exception;

import br.com.totvs.insight360.dto.resposta.ErroCampo;
import br.com.totvs.insight360.dto.resposta.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * Tratamento centralizado de exceções da API (padrão Controller Advice).
 * <p>
 * Nenhum controller precisa de try/catch: as exceções de negócio sobem até aqui
 * e são convertidas em respostas HTTP padronizadas ({@link ErroResposta}).
 */
@RestControllerAdvice
@Slf4j
public class ManipuladorGlobalExcecoes {

    // ── Exceções de negócio ───────────────────────────────────────────

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException excecao,
                                                                   HttpServletRequest requisicao) {
        return construir(HttpStatus.NOT_FOUND, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(ConflitoDeDadosException.class)
    public ResponseEntity<ErroResposta> tratarConflito(ConflitoDeDadosException excecao,
                                                       HttpServletRequest requisicao) {
        return construir(HttpStatus.CONFLICT, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> tratarRegraNegocio(RegraNegocioException excecao,
                                                           HttpServletRequest requisicao) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> tratarCredenciaisInvalidas(CredenciaisInvalidasException excecao,
                                                                   HttpServletRequest requisicao) {
        return construir(HttpStatus.UNAUTHORIZED, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ResponseEntity<ErroResposta> tratarOperacaoNaoPermitida(OperacaoNaoPermitidaException excecao,
                                                                   HttpServletRequest requisicao) {
        return construir(HttpStatus.FORBIDDEN, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResposta> tratarArquivoInvalido(ArquivoInvalidoException excecao,
                                                              HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST, excecao.getMessage(), requisicao);
    }

    @ExceptionHandler(RelatorioException.class)
    public ResponseEntity<ErroResposta> tratarFalhaRelatorio(RelatorioException excecao,
                                                             HttpServletRequest requisicao) {
        log.error("Falha ao gerar relatorio: {}", excecao.getMessage(), excecao);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, excecao.getMessage(), requisicao);
    }

    // ── Validação de entrada ──────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarCorpoInvalido(MethodArgumentNotValidException excecao,
                                                            HttpServletRequest requisicao) {
        List<ErroCampo> campos = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();
        ErroResposta corpo = ErroResposta.deValidacao(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Existem campos inválidos na requisição.",
                requisicao.getRequestURI(),
                campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResposta> tratarViolacaoDeRestricao(ConstraintViolationException excecao,
                                                                  HttpServletRequest requisicao) {
        List<ErroCampo> campos = excecao.getConstraintViolations().stream()
                .map(violacao -> new ErroCampo(violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();
        ErroResposta corpo = ErroResposta.deValidacao(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Existem parâmetros inválidos na requisição.",
                requisicao.getRequestURI(),
                campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarTipoInvalido(MethodArgumentTypeMismatchException excecao,
                                                           HttpServletRequest requisicao) {
        String tipoEsperado = excecao.getRequiredType() != null
                ? excecao.getRequiredType().getSimpleName() : "desconhecido";
        String mensagem = "O parâmetro " + excecao.getName() + " recebeu um valor incompatível com o tipo "
                + tipoEsperado + ".";
        return construir(HttpStatus.BAD_REQUEST, mensagem, requisicao);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResposta> tratarParametroAusente(MissingServletRequestParameterException excecao,
                                                               HttpServletRequest requisicao) {
        return construir(HttpStatus.BAD_REQUEST,
                "O parâmetro obrigatório " + excecao.getParameterName() + " não foi informado.", requisicao);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoIlegivel(HttpMessageNotReadableException excecao,
                                                            HttpServletRequest requisicao) {
        log.debug("Corpo da requisicao ilegivel: {}", excecao.getMessage());
        return construir(HttpStatus.BAD_REQUEST,
                "O corpo da requisição está ausente ou mal formatado (JSON inválido).", requisicao);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResposta> tratarUploadGrande(MaxUploadSizeExceededException excecao,
                                                           HttpServletRequest requisicao) {
        return construir(HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo enviado excede o tamanho máximo permitido.", requisicao);
    }

    // ── Persistência e falhas não previstas ───────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> tratarIntegridade(DataIntegrityViolationException excecao,
                                                          HttpServletRequest requisicao) {
        log.warn("Violacao de integridade: {}", excecao.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade do banco de dados.", requisicao);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroInesperado(Exception excecao,
                                                             HttpServletRequest requisicao) {
        log.error("Erro inesperado em {}: {}", requisicao.getRequestURI(), excecao.getMessage(), excecao);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Consulte o log da aplicação para mais detalhes.", requisicao);
    }

    // ── Apoio ─────────────────────────────────────────────────────────

    private ResponseEntity<ErroResposta> construir(HttpStatus status, String mensagem,
                                                   HttpServletRequest requisicao) {
        ErroResposta corpo = ErroResposta.de(status.value(), status.getReasonPhrase(),
                mensagem, requisicao.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
