package br.com.totvs.insight360.controller;

import br.com.totvs.insight360.dto.mapper.UsuarioMapper;
import br.com.totvs.insight360.dto.requisicao.LoginRequisicao;
import br.com.totvs.insight360.dto.resposta.UsuarioResposta;
import br.com.totvs.insight360.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticação de usuários da plataforma.
 */
@RestController
@RequestMapping("/api/v1/autenticacao")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login dos usuários da plataforma")
public class AutenticacaoController {

    private final UsuarioService usuarioService;

    /**
     * Valida as credenciais e devolve os dados do usuário autenticado.
     * O identificador retornado é usado pelo Front-End nas operações que exigem
     * autoria (comentários e planos de ação).
     */
    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário pelo e-mail e senha")
    public ResponseEntity<UsuarioResposta> login(@RequestBody @Valid LoginRequisicao requisicao) {
        return ResponseEntity.ok(UsuarioMapper.paraResposta(usuarioService.autenticar(requisicao)));
    }
}
