package br.com.totvs.hermes.repository;

import br.com.totvs.hermes.model.Insight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA para {@link Insight}.
 */
public interface InsightRepository extends JpaRepository<Insight, Long> {

    List<Insight> findByReuniaoId(Long reuniaoId);

    void deleteByReuniaoId(Long reuniaoId);
}