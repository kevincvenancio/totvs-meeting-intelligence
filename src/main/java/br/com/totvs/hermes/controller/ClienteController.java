package br.com.totvs.hermes.controller;

import br.com.totvs.hermes.dto.mapper.ClienteMapper;
import br.com.totvs.hermes.dto.mapper.ReuniaoMapper;
import br.com.totvs.hermes.dto.requisicao.ClienteRequisicao;
import br.com.totvs.hermes.dto.resposta.ClienteResposta;
import br.com.totvs.hermes.dto.resposta.PaginaResposta;
import br.com.totvs.hermes.dto.resposta.ReuniaoResumoResposta;
import br.com.totvs.hermes.model.Cliente;
import br.com.totvs.hermes.model.StatusCliente;
import br.com.totvs.hermes.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * CRUD da carteira de clientes.
 */
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Clientes", description = "Cadastro da carteira de clientes e vínculo com as reuniões")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(summary = "Lista clientes de forma paginada, com pesquisa por nome ou CNPJ")
    public ResponseEntity<PaginaResposta<ClienteResposta>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) StatusCliente status,
            @RequestParam(required = false) String uf,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página mínima é 0.") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "O tamanho mínimo da página é 1.")
            @Max(value = 100, message = "O tamanho máximo da página é 100.") int tamanho) {

        List<ClienteResposta> conteudo =
                ClienteMapper.paraLista(clienteService.pesquisar(termo, status, uf, pagina, tamanho));
        long total = clienteService.contarPesquisa(termo, status, uf);
        return ResponseEntity.ok(PaginaResposta.de(conteudo, pagina, tamanho, total));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo identificador")
    public ResponseEntity<ClienteResposta> buscar(@PathVariable Long id) {
        Cliente cliente = clienteService.buscarPorId(id);
        return ResponseEntity.ok(ClienteMapper.paraResposta(cliente, clienteService.contarReunioes(id)));
    }

    @GetMapping("/cnpj/{cnpj}")
    @Operation(summary = "Busca um cliente pelo CNPJ, com ou sem máscara")
    public ResponseEntity<ClienteResposta> buscarPorCnpj(@PathVariable String cnpj) {
        Cliente cliente = clienteService.buscarPorCnpj(cnpj);
        return ResponseEntity.ok(
                ClienteMapper.paraResposta(cliente, clienteService.contarReunioes(cliente.getId())));
    }

    @GetMapping("/{id}/reunioes")
    @Operation(summary = "Lista as reuniões vinculadas a um cliente")
    public ResponseEntity<List<ReuniaoResumoResposta>> listarReunioes(@PathVariable Long id) {
        return ResponseEntity.ok(ReuniaoMapper.paraLista(clienteService.listarReunioes(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastra um cliente validando CNPJ e unicidade")
    public ResponseEntity<ClienteResposta> cadastrar(@RequestBody @Valid ClienteRequisicao requisicao) {
        Cliente cliente = clienteService.cadastrar(requisicao);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cliente.getId())
                .toUri();
        return ResponseEntity.created(localizacao).body(ClienteMapper.paraResposta(cliente, 0L));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um cliente")
    public ResponseEntity<ClienteResposta> atualizar(@PathVariable Long id,
                                                     @RequestBody @Valid ClienteRequisicao requisicao) {
        Cliente cliente = clienteService.atualizar(id, requisicao);
        return ResponseEntity.ok(ClienteMapper.paraResposta(cliente, clienteService.contarReunioes(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um cliente, desvinculando as reuniões associadas")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        clienteService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
