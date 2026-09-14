package br.com.totvs.hermes.repository;

import br.com.totvs.hermes.model.ImportacaoLote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para {@link ImportacaoLote}.
 */
public interface ImportacaoLoteRepository extends JpaRepository<ImportacaoLote, Long> {

    List<ImportacaoLote> findAllByOrderByDataHoraImportacaoDesc();

    Optional<ImportacaoLote> findTopByOrderByDataHoraImportacaoDesc();

    Optional<ImportacaoLote> findByCodigoLote(String codigoLote);

    Optional<ImportacaoLote> findByHashArquivo(String hash);
}