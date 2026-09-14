package br.com.totvs.hermes.repository;

import br.com.totvs.hermes.model.FeedbackReuniao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para {@link FeedbackReuniao}.
 */
public interface FeedbackReuniaoRepository extends JpaRepository<FeedbackReuniao, Long> {

    Optional<FeedbackReuniao> findByReuniaoId(Long reuniaoId);

    List<FeedbackReuniao> findAllByOrderByNivelCriticidadeDesc();
}
