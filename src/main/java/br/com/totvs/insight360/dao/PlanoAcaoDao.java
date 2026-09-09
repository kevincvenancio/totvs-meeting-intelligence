package br.com.totvs.insight360.dao;

import br.com.totvs.insight360.dto.filtro.FiltroPlanoAcao;
import br.com.totvs.insight360.model.PlanoAcao;
import br.com.totvs.insight360.model.StatusPlanoAcao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Operações de acesso a dados de {@link PlanoAcao}.
 */
public interface PlanoAcaoDao extends GenericDao<PlanoAcao, Long> {

    /**
     * Pesquisa paginada aplicando apenas os critérios preenchidos no filtro.
     *
     * @param filtro  critérios de pesquisa
     * @param pagina  número da página (base zero)
     * @param tamanho quantidade de registros por página
     */
    List<PlanoAcao> pesquisar(FiltroPlanoAcao filtro, int pagina, int tamanho);

    /** Total de registros correspondentes ao filtro informado. */
    long contar(FiltroPlanoAcao filtro);

    /** Busca o plano já com reunião e responsável carregados. */
    Optional<PlanoAcao> buscarDetalhado(Long id);

    /** Planos criados a partir de uma reunião. */
    List<PlanoAcao> listarPorReuniao(Long reuniaoId);

    /** Planos atribuídos a um responsável. */
    List<PlanoAcao> listarPorResponsavel(Long responsavelId);

    /** Planos em aberto com prazo vencido, do mais antigo para o mais recente. */
    List<PlanoAcao> listarAtrasados();

    /** Quantidade de planos em cada status — usado pelo painel de indicadores. */
    Map<StatusPlanoAcao, Long> contarPorStatus();

    /** Quantidade de planos em aberto com prazo vencido. */
    long contarAtrasados();

    /** Indica se o responsável possui planos em aberto (bloqueia a exclusão do usuário). */
    boolean existeEmAbertoParaResponsavel(Long responsavelId);

    /** Remove todos os planos de uma reunião (usado na exclusão em cascata). */
    int removerPorReuniao(Long reuniaoId);
}
