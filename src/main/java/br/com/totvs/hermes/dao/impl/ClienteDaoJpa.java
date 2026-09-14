package br.com.totvs.hermes.dao.impl;

import br.com.totvs.hermes.dao.AbstractJpaDao;
import br.com.totvs.hermes.dao.ClienteDao;
import br.com.totvs.hermes.model.Cliente;
import br.com.totvs.hermes.model.StatusCliente;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação JPA de {@link ClienteDao}.
 */
@Repository
public class ClienteDaoJpa extends AbstractJpaDao<Cliente, Long> implements ClienteDao {

    private static final String SELECT_BASE = "SELECT c FROM Cliente c";
    private static final String COUNT_BASE = "SELECT COUNT(c) FROM Cliente c";

    public ClienteDaoJpa() {
        super(Cliente.class);
    }

    @Override
    public Optional<Cliente> buscarPorCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return Optional.empty();
        }
        return primeiroResultado(criarConsulta(SELECT_BASE + " WHERE c.cnpj = :cnpj")
                .setParameter("cnpj", cnpj.trim()));
    }

    @Override
    public boolean existeOutroComCnpj(String cnpj, Long idIgnorado) {
        if (cnpj == null || cnpj.isBlank()) {
            return false;
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("cnpj", cnpj.trim());
        String jpql = COUNT_BASE + " WHERE c.cnpj = :cnpj";
        if (idIgnorado != null) {
            jpql += " AND c.id <> :idIgnorado";
            parametros.put("idIgnorado", idIgnorado);
        }
        return contarPor(jpql, parametros) > 0;
    }

    @Override
    public List<Cliente> pesquisar(String termo, StatusCliente status, String uf, int pagina, int tamanho) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(termo, status, uf, parametros);
        return paginar(criarConsulta(SELECT_BASE + clausulas + " ORDER BY c.razaoSocial", parametros),
                pagina, tamanho).getResultList();
    }

    @Override
    public long contarPesquisa(String termo, StatusCliente status, String uf) {
        Map<String, Object> parametros = new HashMap<>();
        String clausulas = montarClausulas(termo, status, uf, parametros);
        return contarPor(COUNT_BASE + clausulas, parametros);
    }

    @Override
    public long contarPorStatus(StatusCliente status) {
        return contarPor(COUNT_BASE + " WHERE c.status = :status", Map.of("status", status));
    }

    /** Monta o WHERE dinâmico compartilhado entre a pesquisa e a contagem. */
    private String montarClausulas(String termo, StatusCliente status, String uf,
                                   Map<String, Object> parametros) {
        StringBuilder clausulas = new StringBuilder(" WHERE 1 = 1");
        if (termo != null && !termo.isBlank()) {
            clausulas.append(" AND (LOWER(c.razaoSocial) LIKE :termo")
                    .append(" OR LOWER(c.nomeFantasia) LIKE :termo")
                    .append(" OR c.cnpj LIKE :termo)");
            parametros.put("termo", "%" + termo.trim().toLowerCase() + "%");
        }
        if (status != null) {
            clausulas.append(" AND c.status = :status");
            parametros.put("status", status);
        }
        if (uf != null && !uf.isBlank()) {
            clausulas.append(" AND UPPER(c.uf) = :uf");
            parametros.put("uf", uf.trim().toUpperCase());
        }
        return clausulas.toString();
    }
}
