package br.com.totvs.hermes.dao.impl;

import br.com.totvs.hermes.dao.AbstractJpaDao;
import br.com.totvs.hermes.dao.PlanoAcaoDao;
import br.com.totvs.hermes.dto.filtro.FiltroPlanoAcao;
import br.com.totvs.hermes.model.PlanoAcao;
import br.com.totvs.hermes.model.StatusPlanoAcao;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação JPA de {@link PlanoAcaoDao}.
 * <p>
 * As consultas usam {@code JOIN FETCH} em reunião e responsável porque a camada
 * web trabalha com a sessão JPA já encerrada ({@code spring.jpa.open-in-view=false}).
 */
@Repository
public class PlanoAcaoDaoJpa extends AbstractJpaDao<PlanoAcao, Long> implements PlanoAcaoDao {

    private static final String SELECT_BASE = """
            SELECT p FROM PlanoAcao p
            JOIN FETCH p.reuniao reuniao
            JOIN FETCH p.responsavel responsavel""";

    private static final String COUNT_BASE = """
            SELECT COUNT(p) FROM PlanoAcao p
            JOIN p.reuniao reuniao
            JOIN p.responsavel responsavel""";

    private static final List<StatusPlanoAcao> STATUS_EM_ABERTO =
            List.of(StatusPlanoAcao.PENDENTE, StatusPlanoAcao.EM_ANDAMENTO);

    public PlanoAcaoDaoJpa() {
        super(PlanoAcao.class);
    }

    @Override
    public List<PlanoAcao> pesquisar(FiltroPlanoAcao filtro, int pagina, int tamanho) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(filtro, parametros);
        String ordenacao = " ORDER BY p." + filtro.campoOrdenacao() + " " + filtro.direcaoOrdenacao();
        return paginar(criarConsulta(SELECT_BASE + clausulas + ordenacao, parametros), pagina, tamanho)
                .getResultList();
    }

    @Override
    public long contar(FiltroPlanoAcao filtro) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(filtro, parametros);
        return contarPor(COUNT_BASE + clausulas, parametros);
    }

    @Override
    public Optional<PlanoAcao> buscarDetalhado(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return primeiroResultado(criarConsulta(SELECT_BASE + " WHERE p.id = :id").setParameter("id", id));
    }

    @Override
    public List<PlanoAcao> listarPorReuniao(Long reuniaoId) {
        return criarConsulta(SELECT_BASE + " WHERE reuniao.id = :reuniaoId ORDER BY p.prazo")
                .setParameter("reuniaoId", reuniaoId)
                .getResultList();
    }

    @Override
    public List<PlanoAcao> listarPorResponsavel(Long responsavelId) {
        return criarConsulta(SELECT_BASE + " WHERE responsavel.id = :responsavelId ORDER BY p.prazo")
                .setParameter("responsavelId", responsavelId)
                .getResultList();
    }

    @Override
    public List<PlanoAcao> listarAtrasados() {
        return criarConsulta(SELECT_BASE + " WHERE p.status IN :statusEmAberto AND p.prazo < :hoje"
                        + " ORDER BY p.prazo")
                .setParameter("statusEmAberto", STATUS_EM_ABERTO)
                .setParameter("hoje", LocalDate.now())
                .getResultList();
    }

    @Override
    public Map<StatusPlanoAcao, Long> contarPorStatus() {
        List<Object[]> linhas = gerenciadorEntidades
                .createQuery("SELECT p.status, COUNT(p) FROM PlanoAcao p GROUP BY p.status", Object[].class)
                .getResultList();

        Map<StatusPlanoAcao, Long> totais = new EnumMap<>(StatusPlanoAcao.class);
        for (StatusPlanoAcao status : StatusPlanoAcao.values()) {
            totais.put(status, 0L);
        }
        for (Object[] linha : linhas) {
            totais.put((StatusPlanoAcao) linha[0], (Long) linha[1]);
        }
        return totais;
    }

    @Override
    public long contarAtrasados() {
        return contarPor("SELECT COUNT(p) FROM PlanoAcao p"
                        + " WHERE p.status IN :statusEmAberto AND p.prazo < :hoje",
                Map.of("statusEmAberto", STATUS_EM_ABERTO, "hoje", LocalDate.now()));
    }

    @Override
    public boolean existeEmAbertoParaResponsavel(Long responsavelId) {
        return contarPor("SELECT COUNT(p) FROM PlanoAcao p"
                        + " WHERE p.responsavel.id = :responsavelId AND p.status IN :statusEmAberto",
                Map.of("responsavelId", responsavelId, "statusEmAberto", STATUS_EM_ABERTO)) > 0;
    }

    @Override
    public int removerPorReuniao(Long reuniaoId) {
        return gerenciadorEntidades
                .createQuery("DELETE FROM PlanoAcao p WHERE p.reuniao.id = :reuniaoId")
                .setParameter("reuniaoId", reuniaoId)
                .executeUpdate();
    }

    /** Monta o WHERE dinâmico compartilhado entre a pesquisa e a contagem. */
    private String montarClausulas(FiltroPlanoAcao filtro, Map<String, Object> parametros) {
        StringBuilder clausulas = new StringBuilder(" WHERE 1 = 1");

        if (filtro.termo() != null && !filtro.termo().isBlank()) {
            clausulas.append(" AND (LOWER(p.titulo) LIKE :termo OR LOWER(p.descricao) LIKE :termo)");
            parametros.put("termo", "%" + filtro.termo().trim().toLowerCase() + "%");
        }
        if (filtro.reuniaoId() != null) {
            clausulas.append(" AND reuniao.id = :reuniaoId");
            parametros.put("reuniaoId", filtro.reuniaoId());
        }
        if (filtro.responsavelId() != null) {
            clausulas.append(" AND responsavel.id = :responsavelId");
            parametros.put("responsavelId", filtro.responsavelId());
        }
        if (filtro.status() != null) {
            clausulas.append(" AND p.status = :status");
            parametros.put("status", filtro.status());
        }
        if (filtro.prioridade() != null) {
            clausulas.append(" AND p.prioridade = :prioridade");
            parametros.put("prioridade", filtro.prioridade());
        }
        if (filtro.isSomenteAtrasados()) {
            clausulas.append(" AND p.status IN :statusEmAberto AND p.prazo < :hoje");
            parametros.put("statusEmAberto", STATUS_EM_ABERTO);
            parametros.put("hoje", LocalDate.now());
        }
        if (filtro.prazoAte() != null) {
            clausulas.append(" AND p.prazo <= :prazoAte");
            parametros.put("prazoAte", filtro.prazoAte());
        }
        return clausulas.toString();
    }
}
