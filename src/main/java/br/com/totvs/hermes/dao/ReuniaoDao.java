package br.com.totvs.hermes.dao;

import br.com.totvs.hermes.dto.filtro.FiltroReuniao;
import br.com.totvs.hermes.model.Reuniao;

import java.util.List;
import java.util.Optional;

/**
 * Operações de acesso a dados de {@link Reuniao} que complementam o repositório
 * Spring Data existente, com consultas dinâmicas e paginadas para o Front-End.
 */
public interface ReuniaoDao extends GenericDao<Reuniao, Long> {

    /**
     * Pesquisa paginada aplicando apenas os critérios preenchidos no filtro.
     *
     * @param filtro  critérios de pesquisa
     * @param pagina  número da página (base zero)
     * @param tamanho quantidade de registros por página
     */
    List<Reuniao> pesquisar(FiltroReuniao filtro, int pagina, int tamanho);

    /** Total de registros correspondentes ao filtro informado. */
    long contar(FiltroReuniao filtro);

    /** Busca a reunião já com lote e cliente carregados, para montagem do detalhe. */
    Optional<Reuniao> buscarDetalhada(Long id);

    /** Reuniões vinculadas a um cliente cadastrado. */
    List<Reuniao> listarPorCliente(Long clienteId);

    /** Quantidade de reuniões vinculadas a um cliente cadastrado. */
    long contarPorCliente(Long clienteId);

    /** Desfaz o vínculo das reuniões com um cliente que será removido. */
    int desvincularCliente(Long clienteId);
}
