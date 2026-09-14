package br.com.totvs.hermes.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação base da camada DAO sobre a JPA (padrão <i>Template Method</i>).
 * <p>
 * Reúne o CRUD comum a todas as entidades; os DAOs concretos herdam essa
 * implementação e acrescentam apenas as consultas específicas do seu domínio.
 * As operações de escrita exigem uma transação ativa, aberta pela camada de
 * serviço através de {@code @Transactional}.
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo do identificador da entidade
 */
public abstract class AbstractJpaDao<T, ID extends Serializable> implements GenericDao<T, ID> {

    @PersistenceContext
    protected EntityManager gerenciadorEntidades;

    private final Class<T> classeEntidade;

    protected AbstractJpaDao(Class<T> classeEntidade) {
        this.classeEntidade = classeEntidade;
    }

    @Override
    public T inserir(T entidade) {
        gerenciadorEntidades.persist(entidade);
        return entidade;
    }

    @Override
    public T atualizar(T entidade) {
        return gerenciadorEntidades.merge(entidade);
    }

    @Override
    public Optional<T> buscarPorId(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(gerenciadorEntidades.find(classeEntidade, id));
    }

    @Override
    public List<T> listarTodos() {
        return criarConsulta("SELECT e FROM " + nomeEntidade() + " e ORDER BY e.id").getResultList();
    }

    @Override
    public void remover(T entidade) {
        if (entidade == null) {
            return;
        }
        T gerenciada = gerenciadorEntidades.contains(entidade)
                ? entidade
                : gerenciadorEntidades.merge(entidade);
        gerenciadorEntidades.remove(gerenciada);
    }

    @Override
    public boolean removerPorId(ID id) {
        Optional<T> encontrada = buscarPorId(id);
        encontrada.ifPresent(this::remover);
        return encontrada.isPresent();
    }

    @Override
    public boolean existePorId(ID id) {
        if (id == null) {
            return false;
        }
        Long total = gerenciadorEntidades
                .createQuery("SELECT COUNT(e) FROM " + nomeEntidade() + " e WHERE e.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return total != null && total > 0;
    }

    @Override
    public long contar() {
        return gerenciadorEntidades
                .createQuery("SELECT COUNT(e) FROM " + nomeEntidade() + " e", Long.class)
                .getSingleResult();
    }

    // ── Apoio para os DAOs concretos ──────────────────────────────────

    /** Nome da entidade usado nas consultas JPQL. */
    protected String nomeEntidade() {
        return classeEntidade.getSimpleName();
    }

    /** Cria uma consulta tipada para a entidade do DAO. */
    protected TypedQuery<T> criarConsulta(String jpql) {
        return gerenciadorEntidades.createQuery(jpql, classeEntidade);
    }

    /** Cria uma consulta tipada já com os parâmetros aplicados. */
    protected TypedQuery<T> criarConsulta(String jpql, Map<String, Object> parametros) {
        TypedQuery<T> consulta = criarConsulta(jpql);
        parametros.forEach(consulta::setParameter);
        return consulta;
    }

    /**
     * Executa a consulta esperando no máximo um resultado.
     * Evita a {@code NoResultException} da JPA, devolvendo um {@link Optional}.
     */
    protected Optional<T> primeiroResultado(TypedQuery<T> consulta) {
        List<T> resultados = consulta.setMaxResults(1).getResultList();
        return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
    }

    /** Aplica os limites de paginação a uma consulta. */
    protected TypedQuery<T> paginar(TypedQuery<T> consulta, int pagina, int tamanho) {
        return consulta.setFirstResult(pagina * tamanho).setMaxResults(tamanho);
    }

    /** Conta os registros de uma consulta JPQL de contagem. */
    protected long contarPor(String jpql, Map<String, Object> parametros) {
        var consulta = gerenciadorEntidades.createQuery(jpql, Long.class);
        parametros.forEach(consulta::setParameter);
        Long total = consulta.getSingleResult();
        return total != null ? total : 0L;
    }
}
