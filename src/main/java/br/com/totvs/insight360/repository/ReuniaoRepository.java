package br.com.totvs.insight360.repository;

import br.com.totvs.insight360.model.Reuniao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReuniaoRepository extends JpaRepository<Reuniao, Long> {

    List<Reuniao> findByAnalisadaTrue();

    List<Reuniao> findByClienteContainingIgnoreCase(String cliente);

    @Query("SELECT r FROM Reuniao r WHERE r.riscoChurn = 'ALTO' AND r.analisada = true AND (r.duplicada = false OR r.duplicada IS NULL) ORDER BY r.scoreComercial DESC")
    List<Reuniao> findByAltoRiscoChurn();

    @Query("SELECT r FROM Reuniao r WHERE r.analisada = true AND (r.duplicada = false OR r.duplicada IS NULL) ORDER BY r.scoreComercial DESC NULLS LAST")
    List<Reuniao> findAllAnalisadasOrderByScore();

    @Query("SELECT r FROM Reuniao r WHERE r.analisada = true AND (r.duplicada = false OR r.duplicada IS NULL) AND r.statusCompletude = :status ORDER BY r.scoreComercial DESC NULLS LAST")
    List<Reuniao> findByStatusCompletude(@Param("status") String status);

    @Query("SELECT r FROM Reuniao r WHERE r.analisada = true AND (r.duplicada = false OR r.duplicada IS NULL) AND r.sentimento = :sentimento ORDER BY r.id DESC")
    List<Reuniao> findBySentimento(@Param("sentimento") String sentimento);

    @Query("SELECT r FROM Reuniao r WHERE r.analisada = true AND (r.duplicada = false OR r.duplicada IS NULL) AND r.prioridade = :prioridade ORDER BY r.scoreComercial DESC NULLS LAST")
    List<Reuniao> findByPrioridade(@Param("prioridade") String prioridade);

    @Query("SELECT COUNT(r) FROM Reuniao r WHERE r.analisada = true")
    long countAnalisadas();

    @Query("SELECT r FROM Reuniao r WHERE r.loteImportacao.id = :loteId ORDER BY r.scoreComercial DESC NULLS LAST")
    List<Reuniao> findByLoteId(@Param("loteId") Long loteId);

    @Query("SELECT r FROM Reuniao r WHERE r.loteImportacao.id = :loteId AND r.analisada = true ORDER BY r.scoreComercial DESC NULLS LAST")
    List<Reuniao> findAnalisadasByLoteId(@Param("loteId") Long loteId);

    @Query("SELECT COUNT(r) FROM Reuniao r WHERE r.loteImportacao.id = :loteId")
    long countByLoteId(@Param("loteId") Long loteId);

    Optional<Reuniao> findTopByIdExternoAndDuplicadaFalse(String idExterno);
}
