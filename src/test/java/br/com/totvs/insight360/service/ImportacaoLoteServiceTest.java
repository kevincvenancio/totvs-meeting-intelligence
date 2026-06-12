package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.ImportacaoLote;
import br.com.totvs.insight360.repository.ImportacaoLoteRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Testes manuais para ImportacaoLoteService.
 * Padrão: apenas try-catch, sem JUnit/AssertJ/Mockito.
 * Usa stub manual de ImportacaoLoteRepository.
 */
public class ImportacaoLoteServiceTest {

    // ── Stub manual de ImportacaoLoteRepository ───────────────────────

    static class ImportacaoLoteRepositoryStub implements ImportacaoLoteRepository {
        @lombok.Getter
        private ImportacaoLote salvo;

        @Override
        public @NonNull <S extends ImportacaoLote> S save(@NonNull S entity) {
            this.salvo = entity;
            return entity;
        }

        @Override public @NonNull List<ImportacaoLote> findAllByOrderByDataHoraImportacaoDesc() { return List.of(); }
        @Override public @NonNull Optional<ImportacaoLote> findTopByOrderByDataHoraImportacaoDesc() { return Optional.empty(); }
        @Override public @NonNull Optional<ImportacaoLote> findByCodigoLote(@NonNull String c) { return Optional.empty(); }
        @Override public @NonNull Optional<ImportacaoLote> findByHashArquivo(@NonNull String h) { return Optional.empty(); }
        @Override public @NonNull <S extends ImportacaoLote> List<S> saveAll(@NonNull Iterable<S> i) { throw new UnsupportedOperationException(); }
        @Override public @NonNull Optional<ImportacaoLote> findById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<ImportacaoLote> findAll() { return List.of(); }
        @Override public @NonNull List<ImportacaoLote> findAllById(@NonNull Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return 0L; }
        @Override public void deleteById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public void delete(@NonNull ImportacaoLote e) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllById(@NonNull Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(@NonNull Iterable<? extends ImportacaoLote> i) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<ImportacaoLote> findAll(@NonNull Sort s) { throw new UnsupportedOperationException(); }
        @Override public @NonNull Page<ImportacaoLote> findAll(@NonNull Pageable p) { throw new UnsupportedOperationException(); }
        @Override public void flush() {}
        @Override public @NonNull <S extends ImportacaoLote> S saveAndFlush(@NonNull S e) { return save(e); }
        @Override public @NonNull <S extends ImportacaoLote> List<S> saveAllAndFlush(@NonNull Iterable<S> i) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(@NonNull Iterable<ImportacaoLote> i) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(@NonNull Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public @NonNull ImportacaoLote getOne(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull ImportacaoLote getById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull ImportacaoLote getReferenceById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends ImportacaoLote> Optional<S> findOne(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends ImportacaoLote> List<S> findAll(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends ImportacaoLote> List<S> findAll(@NonNull Example<S> e, @NonNull Sort s) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends ImportacaoLote> Page<S> findAll(@NonNull Example<S> e, @NonNull Pageable p) { throw new UnsupportedOperationException(); }
        @Override public <S extends ImportacaoLote> long count(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public <S extends ImportacaoLote> boolean exists(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends ImportacaoLote, R> R findBy(@NonNull Example<S> e, @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> f) { throw new UnsupportedOperationException(); }
    }

    // ── Testes ────────────────────────────────────────────────────────

    @SuppressWarnings("ExtractMethodRecommender")
    public static void main(String[] args) {
        int passou = 0, falhou = 0;

        // Teste 1 — finalizarLote seta status "CONCLUIDO" quando há reuniões válidas
        try {
            ImportacaoLoteRepositoryStub stub = new ImportacaoLoteRepositoryStub();
            ImportacaoLoteService service = new ImportacaoLoteService(stub);

            ImportacaoLote lote = new ImportacaoLote();
            lote.setCodigoLote("Lote 001");

            service.finalizarLote(lote, 10, 8, 1, 1, 0, null);

            ImportacaoLote salvo = stub.getSalvo();
            if (salvo == null) throw new AssertionError("Lote não foi salvo no repositório");
            if (!"CONCLUIDO".equals(salvo.getStatusProcessamento()))
                throw new AssertionError("Esperava status 'CONCLUIDO', mas obteve: '" + salvo.getStatusProcessamento() + "'");
            if (salvo.getTotalReunioesValidas() != 8)
                throw new AssertionError("Esperava totalReunioesValidas=8, mas obteve: " + salvo.getTotalReunioesValidas());
            passou++;
            System.out.println("[OK] Teste 1 — finalizarLote seta status CONCLUIDO quando há válidas");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 1 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 1 — exceção inesperada: " + e.getMessage());
        }

        // Teste 2 — finalizarLote seta status "ERRO" quando erros > 0 e validas == 0
        try {
            ImportacaoLoteRepositoryStub stub = new ImportacaoLoteRepositoryStub();
            ImportacaoLoteService service = new ImportacaoLoteService(stub);

            ImportacaoLote lote = new ImportacaoLote();
            lote.setCodigoLote("Lote 002");

            service.finalizarLote(lote, 5, 0, 0, 0, 5, "Todos os registros falharam");

            ImportacaoLote salvo = stub.getSalvo();
            if (salvo == null) throw new AssertionError("Lote não foi salvo no repositório");
            if (!"ERRO".equals(salvo.getStatusProcessamento()))
                throw new AssertionError("Esperava status 'ERRO', mas obteve: '" + salvo.getStatusProcessamento() + "'");
            if (salvo.getTotalReunioesComErro() != 5)
                throw new AssertionError("Esperava totalReunioesComErro=5, mas obteve: " + salvo.getTotalReunioesComErro());
            passou++;
            System.out.println("[OK] Teste 2 — finalizarLote seta status ERRO quando erros > 0 e válidas == 0");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 2 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 2 — exceção inesperada: " + e.getMessage());
        }

        // Teste 3 — finalizarLote com erros E válidas mantém status "CONCLUIDO"
        try {
            ImportacaoLoteRepositoryStub stub = new ImportacaoLoteRepositoryStub();
            ImportacaoLoteService service = new ImportacaoLoteService(stub);

            ImportacaoLote lote = new ImportacaoLote();
            lote.setCodigoLote("Lote 003");

            // erros=2, mas validas=5 — deve ser CONCLUIDO, não ERRO
            service.finalizarLote(lote, 7, 5, 0, 0, 2, "Alguns erros");

            ImportacaoLote salvo = stub.getSalvo();
            if (!"CONCLUIDO".equals(salvo.getStatusProcessamento()))
                throw new AssertionError("Esperava CONCLUIDO (há válidas mesmo com erros), mas obteve: '" + salvo.getStatusProcessamento() + "'");
            passou++;
            System.out.println("[OK] Teste 3 — finalizarLote mantém CONCLUIDO quando há válidas mesmo com erros");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 3 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 3 — exceção inesperada: " + e.getMessage());
        }

        System.out.println("\nResultado: " + passou + " passou(ram) | " + falhou + " falhou(ram)");
    }
}