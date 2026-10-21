package br.com.totvs.hermes.repository;

import br.com.totvs.hermes.model.Reuniao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReuniaoRepository extends JpaRepository<Reuniao, Long> {

    List<Reuniao> findByAnalisadaTrue();

    /** Colunas usadas pelo dashboard, sem CLOBs: ver {@link IndicadoresReuniao}. */
    @Query("""
            SELECT new br.com.totvs.hermes.repository.IndicadoresReuniao(
                r.id, r.cliente, r.riscoChurn, r.sentimento, r.scoreQualidade, r.scoreComercial,
                r.produtosIdentificados, r.concorrentesIdentificados, r.categoriasPrincipais,
                CASE WHEN LENGTH(r.oportunidades) > 0 THEN 1 ELSE 0 END)
            FROM Reuniao r WHERE r.analisada = true ORDER BY r.id""")
    List<IndicadoresReuniao> findIndicadoresAnalisadas();

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

    /**
     * Reuniões que a fila de IA deve analisar: analisadas, não duplicadas e com conteúdo.
     *
     * @param todas {@code false} = só as que ainda não passaram pela IA
     */
    @Query("""
            SELECT r.id FROM Reuniao r
            WHERE r.analisada = true AND (r.duplicada = false OR r.duplicada IS NULL)
              AND (r.statusCompletude IS NULL OR r.statusCompletude <> 'INCOMPLETA')
              AND (:todas = true OR r.origemAnalise IS NULL OR r.origemAnalise IN ('REGRAS', 'IA_PENDENTE'))
            ORDER BY r.id""")
    List<Long> findIdsParaAnaliseIa(@Param("todas") boolean todas);
}
