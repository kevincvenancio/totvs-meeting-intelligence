package br.com.totvs.insight360.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico da camada DAO (padrão <i>Data Access Object</i>).
 * <p>
 * Concentra as operações de CRUD comuns a qualquer entidade, de modo que os DAOs
 * específicos só precisem declarar as consultas próprias do seu domínio.
 * O controle transacional é responsabilidade da camada de serviço.
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo do identificador da entidade
 */
public interface GenericDao<T, ID extends Serializable> {

    /**
     * Persiste uma nova entidade.
     *
     * @param entidade entidade ainda sem identificador
     * @return a entidade gerenciada, já com o id preenchido
     */
    T inserir(T entidade);

    /**
     * Sincroniza o estado de uma entidade já existente.
     *
     * @param entidade entidade com o id preenchido
     * @return a instância gerenciada resultante do merge
     */
    T atualizar(T entidade);

    /**
     * Busca uma entidade pelo identificador.
     *
     * @param id identificador procurado
     * @return {@link Optional} vazio quando não existe registro com o id
     */
    Optional<T> buscarPorId(ID id);

    /** Lista todas as entidades ordenadas pelo identificador. */
    List<T> listarTodos();

    /** Remove a entidade informada. */
    void remover(T entidade);

    /**
     * Remove a entidade correspondente ao id.
     *
     * @return {@code true} quando havia registro para remover
     */
    boolean removerPorId(ID id);

    /** Indica se existe registro com o id informado. */
    boolean existePorId(ID id);

    /** Total de registros da entidade. */
    long contar();
}
