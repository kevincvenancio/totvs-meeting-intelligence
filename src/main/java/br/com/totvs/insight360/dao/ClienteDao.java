package br.com.totvs.insight360.dao;

import br.com.totvs.insight360.model.Cliente;
import br.com.totvs.insight360.model.StatusCliente;

import java.util.List;
import java.util.Optional;

/**
 * Operações de acesso a dados de {@link Cliente}.
 */
public interface ClienteDao extends GenericDao<Cliente, Long> {

    /** Busca um cliente pelo CNPJ (apenas dígitos). */
    Optional<Cliente> buscarPorCnpj(String cnpj);

    /**
     * Verifica se o CNPJ já pertence a outro cliente.
     *
     * @param cnpj       CNPJ a validar (apenas dígitos)
     * @param idIgnorado id do próprio cliente em edição ({@code null} na inclusão)
     */
    boolean existeOutroComCnpj(String cnpj, Long idIgnorado);

    /**
     * Pesquisa paginada por razão social, nome fantasia ou CNPJ.
     *
     * @param termo  texto livre (opcional)
     * @param status filtro por situação comercial (opcional)
     * @param uf     filtro por unidade federativa (opcional)
     */
    List<Cliente> pesquisar(String termo, StatusCliente status, String uf, int pagina, int tamanho);

    /** Total de registros correspondentes aos mesmos critérios de {@link #pesquisar}. */
    long contarPesquisa(String termo, StatusCliente status, String uf);

    /** Quantidade de clientes em uma determinada situação. */
    long contarPorStatus(StatusCliente status);
}
