package br.com.totvs.hermes.repository;

import br.com.totvs.hermes.model.Insight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

/**
 * Repositório JPA para {@link Insight}.
 */
public interface InsightRepository extends JpaRepository<Insight, Long> {

    List<Insight> findByReuniaoId(Long reuniaoId);

    void deleteByReuniaoId(Long reuniaoId);

    /**
     * Apaga os insights que a IA substitui ao reanalisar: os da própria IA (tipo
     * terminado em "(IA)") e os de churn das regras ("Risco de churn — ALTO/MÉDIO").
     */
    @Modifying
    @Query("DELETE FROM Insight i WHERE i.reuniao.id = :reuniaoId AND (i.tipo LIKE :sufixoIa OR i.tipo LIKE :prefixoChurn)")
    int deleteSubstituidosPelaIa(@Param("reuniaoId") Long reuniaoId,
                                 @Param("sufixoIa") String sufixoIa,
                                 @Param("prefixoChurn") String prefixoChurn);
}