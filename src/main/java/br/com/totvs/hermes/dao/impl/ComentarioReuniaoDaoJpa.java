package br.com.totvs.hermes.dao.impl;

import br.com.totvs.hermes.dao.AbstractJpaDao;
import br.com.totvs.hermes.dao.ComentarioReuniaoDao;
import br.com.totvs.hermes.model.ComentarioReuniao;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação JPA de {@link ComentarioReuniaoDao}.
 */
@Repository
public class ComentarioReuniaoDaoJpa extends AbstractJpaDao<ComentarioReuniao, Long>
        implements ComentarioReuniaoDao {

    private static final String SELECT_BASE = """
            SELECT c FROM ComentarioReuniao c
            JOIN FETCH c.reuniao reuniao
            JOIN FETCH c.autor autor""";

    private static final String ORDEM_PADRAO = " ORDER BY c.dataCriacao DESC";

    public ComentarioReuniaoDaoJpa() {
        super(ComentarioReuniao.class);
    }

    @Override
    public List<ComentarioReuniao> listarPorReuniao(Long reuniaoId) {
        return consultaPorReuniao(reuniaoId).getResultList();
    }

    @Override
    public List<ComentarioReuniao> listarPorReuniao(Long reuniaoId, int pagina, int tamanho) {
        return paginar(consultaPorReuniao(reuniaoId), pagina, tamanho).getResultList();
    }

    @Override
    public Optional<ComentarioReuniao> buscarDetalhado(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return primeiroResultado(criarConsulta(SELECT_BASE + " WHERE c.id = :id").setParameter("id", id));
    }

    @Override
    public long contarPorReuniao(Long reuniaoId) {
        return contarPor("SELECT COUNT(c) FROM ComentarioReuniao c WHERE c.reuniao.id = :reuniaoId",
                Map.of("reuniaoId", reuniaoId));
    }

    @Override
    public long contarPorAutor(Long autorId) {
        return contarPor("SELECT COUNT(c) FROM ComentarioReuniao c WHERE c.autor.id = :autorId",
                Map.of("autorId", autorId));
    }

    @Override
    public int removerPorReuniao(Long reuniaoId) {
        return gerenciadorEntidades
                .createQuery("DELETE FROM ComentarioReuniao c WHERE c.reuniao.id = :reuniaoId")
                .setParameter("reuniaoId", reuniaoId)
                .executeUpdate();
    }

    private TypedQuery<ComentarioReuniao> consultaPorReuniao(Long reuniaoId) {
        return criarConsulta(SELECT_BASE + " WHERE reuniao.id = :reuniaoId" + ORDEM_PADRAO)
                .setParameter("reuniaoId", reuniaoId);
    }
}
