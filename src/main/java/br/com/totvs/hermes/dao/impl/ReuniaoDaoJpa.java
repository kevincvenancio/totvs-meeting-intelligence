package br.com.totvs.hermes.dao.impl;

import br.com.totvs.hermes.dao.AbstractJpaDao;
import br.com.totvs.hermes.dao.ReuniaoDao;
import br.com.totvs.hermes.dto.filtro.FiltroReuniao;
import br.com.totvs.hermes.model.Reuniao;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação JPA de {@link ReuniaoDao}.
 * <p>
 * As consultas são montadas dinamicamente a partir do {@link FiltroReuniao}:
 * apenas os critérios preenchidos entram no JPQL, e todos os valores viajam como
 * parâmetros nomeados. Os campos de ordenação passam por lista branca no próprio
 * filtro, de forma que nenhuma entrada do usuário é concatenada na consulta.
 */
@Repository
public class ReuniaoDaoJpa extends AbstractJpaDao<Reuniao, Long> implements ReuniaoDao {

    private static final String SELECT_BASE = """
            SELECT r FROM Reuniao r
            LEFT JOIN FETCH r.loteImportacao lote
            LEFT JOIN FETCH r.clienteVinculado cliente""";

    private static final String COUNT_BASE = """
            SELECT COUNT(r) FROM Reuniao r
            LEFT JOIN r.loteImportacao lote
            LEFT JOIN r.clienteVinculado cliente""";

    public ReuniaoDaoJpa() {
        super(Reuniao.class);
    }

    @Override
    public List<Reuniao> pesquisar(FiltroReuniao filtro, int pagina, int tamanho) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(filtro, parametros);
        String ordenacao = " ORDER BY r." + filtro.campoOrdenacao() + " " + filtro.direcaoOrdenacao()
                + " NULLS LAST";
        return paginar(criarConsulta(SELECT_BASE + clausulas + ordenacao, parametros), pagina, tamanho)
                .getResultList();
    }

    @Override
    public long contar(FiltroReuniao filtro) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(filtro, parametros);
        return contarPor(COUNT_BASE + clausulas, parametros);
    }

    @Override
    public Optional<Reuniao> buscarDetalhada(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return primeiroResultado(criarConsulta(SELECT_BASE + " WHERE r.id = :id")
                .setParameter("id", id));
    }

    @Override
    public List<Reuniao> listarPorCliente(Long clienteId) {
        return criarConsulta(SELECT_BASE + " WHERE cliente.id = :clienteId ORDER BY r.data DESC NULLS LAST")
                .setParameter("clienteId", clienteId)
                .getResultList();
    }

    @Override
    public long contarPorCliente(Long clienteId) {
        return contarPor("SELECT COUNT(r) FROM Reuniao r WHERE r.clienteVinculado.id = :clienteId",
                Map.of("clienteId", clienteId));
    }

    @Override
    public int desvincularCliente(Long clienteId) {
        return gerenciadorEntidades
                .createQuery("UPDATE Reuniao r SET r.clienteVinculado = NULL WHERE r.clienteVinculado.id = :clienteId")
                .setParameter("clienteId", clienteId)
                .executeUpdate();
    }

    /** Monta o WHERE dinâmico compartilhado entre a pesquisa e a contagem. */
    private String montarClausulas(FiltroReuniao filtro, Map<String, Object> parametros) {
        StringBuilder clausulas = new StringBuilder(" WHERE 1 = 1");

        if (!filtro.isIncluirDuplicadas()) {
            clausulas.append(" AND (r.duplicada = FALSE OR r.duplicada IS NULL)");
        }
        if (filtro.isSomenteAnalisadas()) {
            clausulas.append(" AND r.analisada = TRUE");
        }
        if (filtro.termo() != null && !filtro.termo().isBlank()) {
            clausulas.append(" AND (LOWER(r.cliente) LIKE :termo")
                    .append(" OR LOWER(r.temaReuniao) LIKE :termo")
                    .append(" OR LOWER(r.resumoReuniao) LIKE :termo")
                    .append(" OR LOWER(r.vendedor) LIKE :termo")
                    .append(" OR LOWER(r.idExterno) LIKE :termo)");
            parametros.put("termo", "%" + filtro.termo().trim().toLowerCase() + "%");
        }
        if (filtro.clienteId() != null) {
            clausulas.append(" AND cliente.id = :clienteId");
            parametros.put("clienteId", filtro.clienteId());
        }
        if (filtro.loteId() != null) {
            clausulas.append(" AND lote.id = :loteId");
            parametros.put("loteId", filtro.loteId());
        }
        if (filtro.statusCompletude() != null) {
            clausulas.append(" AND r.statusCompletude = :statusCompletude");
            parametros.put("statusCompletude", filtro.statusCompletude());
        }
        if (filtro.sentimento() != null) {
            clausulas.append(" AND r.sentimento = :sentimento");
            parametros.put("sentimento", filtro.sentimento());
        }
        if (filtro.riscoChurn() != null) {
            clausulas.append(" AND r.riscoChurn = :riscoChurn");
            parametros.put("riscoChurn", filtro.riscoChurn());
        }
        if (filtro.prioridade() != null && !filtro.prioridade().isBlank()) {
            clausulas.append(" AND UPPER(r.prioridade) = :prioridade");
            parametros.put("prioridade", filtro.prioridade().trim().toUpperCase());
        }
        if (filtro.dataInicial() != null) {
            clausulas.append(" AND r.data >= :dataInicial");
            parametros.put("dataInicial", filtro.dataInicial());
        }
        if (filtro.dataFinal() != null) {
            clausulas.append(" AND r.data <= :dataFinal");
            parametros.put("dataFinal", filtro.dataFinal());
        }
        return clausulas.toString();
    }
}
