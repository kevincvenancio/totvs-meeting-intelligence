package br.com.totvs.insight360.dto.mapper;

import br.com.totvs.insight360.dto.requisicao.ClienteRequisicao;
import br.com.totvs.insight360.dto.resposta.ClienteResposta;
import br.com.totvs.insight360.dto.resposta.ClienteResumoResposta;
import br.com.totvs.insight360.model.Cliente;
import br.com.totvs.insight360.model.StatusCliente;

import java.util.List;

/**
 * Conversões entre {@link Cliente} e seus DTOs (padrão Mapper/Assembler).
 */
public final class ClienteMapper {

    private ClienteMapper() {
        throw new UnsupportedOperationException("Classe utilitária não deve ser instanciada.");
    }

    /**
     * Aplica os dados da requisição sobre a entidade.
     *
     * @param cnpjNormalizado CNPJ já validado e sem máscara pela camada de serviço
     */
    public static void aplicar(ClienteRequisicao requisicao, Cliente cliente, String cnpjNormalizado) {
        cliente.setRazaoSocial(requisicao.razaoSocial().trim());
        cliente.setNomeFantasia(textoOuNulo(requisicao.nomeFantasia()));
        cliente.setCnpj(cnpjNormalizado);
        cliente.setSegmento(textoOuNulo(requisicao.segmento()));
        cliente.setUf(requisicao.uf() != null && !requisicao.uf().isBlank()
                ? requisicao.uf().trim().toUpperCase() : null);
        cliente.setCidade(textoOuNulo(requisicao.cidade()));
        cliente.setFaixaFaturamento(textoOuNulo(requisicao.faixaFaturamento()));
        cliente.setNotaNps(requisicao.notaNps());
        cliente.setStatus(requisicao.status() != null ? requisicao.status() : StatusCliente.PROSPECT);
        cliente.setContatoPrincipal(textoOuNulo(requisicao.contatoPrincipal()));
        cliente.setEmailContato(requisicao.emailContato() != null && !requisicao.emailContato().isBlank()
                ? requisicao.emailContato().trim().toLowerCase() : null);
        cliente.setTelefoneContato(textoOuNulo(requisicao.telefoneContato()));
        cliente.setObservacoes(textoOuNulo(requisicao.observacoes()));
    }

    public static ClienteResposta paraResposta(Cliente cliente, Long totalReunioes) {
        return new ClienteResposta(
                cliente.getId(),
                cliente.getRazaoSocial(),
                cliente.getNomeFantasia(),
                cliente.getNomeExibicao(),
                cliente.getCnpj(),
                cliente.getCnpjFormatado(),
                cliente.getSegmento(),
                cliente.getUf(),
                cliente.getCidade(),
                cliente.getFaixaFaturamento(),
                cliente.getNotaNps(),
                cliente.getStatus(),
                cliente.getStatus() != null ? cliente.getStatus().getDescricao() : null,
                cliente.getContatoPrincipal(),
                cliente.getEmailContato(),
                cliente.getTelefoneContato(),
                cliente.getObservacoes(),
                totalReunioes,
                cliente.getDataCadastro(),
                cliente.getDataAtualizacao());
    }

    public static ClienteResumoResposta paraResumo(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        return new ClienteResumoResposta(cliente.getId(), cliente.getNomeExibicao(),
                cliente.getCnpjFormatado(), cliente.getStatus());
    }

    /** Converte a listagem sem o contador de reuniões, que exigiria uma consulta por item. */
    public static List<ClienteResposta> paraLista(List<Cliente> clientes) {
        return clientes.stream().map(cliente -> paraResposta(cliente, null)).toList();
    }

    private static String textoOuNulo(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
