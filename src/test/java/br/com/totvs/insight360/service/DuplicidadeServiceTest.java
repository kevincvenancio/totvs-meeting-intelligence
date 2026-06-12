package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.ReuniaoRepository;
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
 * Testes manuais para DuplicidadeService.
 * Padrão: apenas try-catch, sem JUnit/AssertJ/Mockito.
 */
public class DuplicidadeServiceTest {

    // ── Stub manual de ReuniaoRepository ─────────────────────────────

    /**
     * Stub que retorna Optional.empty() para findTopByIdExternoAndDuplicadaFalse.
     * Simula banco vazio (nenhuma reunião original encontrada).
     */
    static class ReuniaoRepositoryVazio implements ReuniaoRepository {
        @Override
        public Optional<Reuniao> findTopByIdExternoAndDuplicadaFalse(String idExterno) {
            return Optional.empty();
        }
        // ── Métodos não utilizados neste teste ───────────────────────
        @Override public @NonNull List<Reuniao> findByAnalisadaTrue() { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findByClienteContainingIgnoreCase(@NonNull String c) { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findByAltoRiscoChurn() { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findAllAnalisadasOrderByScore() { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findByStatusCompletude(@NonNull String s) { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findBySentimento(@NonNull String s) { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findByPrioridade(@NonNull String p) { throw new UnsupportedOperationException(); }
        @Override public long countAnalisadas() { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findByLoteId(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findAnalisadasByLoteId(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public long countByLoteId(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao> S save(@NonNull S e) { return e; }
        @Override public @NonNull <S extends Reuniao> List<S> saveAll(@NonNull Iterable<S> i) { throw new UnsupportedOperationException(); }
        @Override public @NonNull Optional<Reuniao> findById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findAll() { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findAllById(@NonNull Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { throw new UnsupportedOperationException(); }
        @Override public void deleteById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public void delete(@NonNull Reuniao e) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllById(@NonNull Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(@NonNull Iterable<? extends Reuniao> i) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { throw new UnsupportedOperationException(); }
        @Override public @NonNull List<Reuniao> findAll(@NonNull Sort s) { throw new UnsupportedOperationException(); }
        @Override public @NonNull Page<Reuniao> findAll(@NonNull Pageable p) { throw new UnsupportedOperationException(); }
        @Override public void flush() { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao> S saveAndFlush(@NonNull S e) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao> List<S> saveAllAndFlush(@NonNull Iterable<S> i) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(@NonNull Iterable<Reuniao> i) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(@NonNull Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public @NonNull Reuniao getOne(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull Reuniao getById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull Reuniao getReferenceById(@NonNull Long id) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao> Optional<S> findOne(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao> List<S> findAll(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao> List<S> findAll(@NonNull Example<S> e, @NonNull Sort s) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao> Page<S> findAll(@NonNull Example<S> e, @NonNull Pageable p) { throw new UnsupportedOperationException(); }
        @Override public <S extends Reuniao> long count(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public <S extends Reuniao> boolean exists(@NonNull Example<S> e) { throw new UnsupportedOperationException(); }
        @Override public @NonNull <S extends Reuniao, R> R findBy(@NonNull Example<S> e, @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> f) { throw new UnsupportedOperationException(); }
    }

    /**
     * Stub que simula banco com uma reunião original já existente para o idExterno dado.
     */
    static class ReuniaoRepositoryComOriginal extends ReuniaoRepositoryVazio {
        private final Reuniao original;
        ReuniaoRepositoryComOriginal(Reuniao original) { this.original = original; }
        @Override
        public Optional<Reuniao> findTopByIdExternoAndDuplicadaFalse(String idExterno) {
            if (idExterno != null && idExterno.equals(original.getIdExterno())) {
                return Optional.of(original);
            }
            return Optional.empty();
        }
    }

    // ── Testes ────────────────────────────────────────────────────────

    public static void main(String[] args) {
        int passou = 0, falhou = 0;

        // Teste 1 — reunião com idExterno nulo não é marcada como duplicata
        try {
            DuplicidadeService service = new DuplicidadeService(new ReuniaoRepositoryVazio());
            Reuniao r = new Reuniao();
            r.setIdExterno(null);

            boolean resultado = service.verificarEMarcar(r);

            if (resultado) throw new AssertionError("Esperava false, mas retornou true para idExterno nulo");
            if (Boolean.TRUE.equals(r.getDuplicada())) throw new AssertionError("Reunião não deveria ser marcada como duplicada");
            passou++;
            System.out.println("[OK] Teste 1 — idExterno nulo não é duplicata");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 1 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 1 — exceção inesperada: " + e.getMessage());
        }

        // Teste 2 — reunião com idExterno vazio não é marcada como duplicata
        try {
            DuplicidadeService service = new DuplicidadeService(new ReuniaoRepositoryVazio());
            Reuniao r = new Reuniao();
            r.setIdExterno("   ");

            boolean resultado = service.verificarEMarcar(r);

            if (resultado) throw new AssertionError("Esperava false, mas retornou true para idExterno vazio");
            passou++;
            System.out.println("[OK] Teste 2 — idExterno vazio não é duplicata");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 2 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 2 — exceção inesperada: " + e.getMessage());
        }

        // Teste 3 — reunião com idExterno novo não é duplicata (banco vazio)
        try {
            DuplicidadeService service = new DuplicidadeService(new ReuniaoRepositoryVazio());
            Reuniao r = new Reuniao();
            r.setIdExterno("MTG-001");

            boolean resultado = service.verificarEMarcar(r);

            if (resultado) throw new AssertionError("Esperava false para idExterno novo, mas retornou true");
            if (Boolean.TRUE.equals(r.getDuplicada())) throw new AssertionError("Reunião não deveria ser marcada como duplicada");
            passou++;
            System.out.println("[OK] Teste 3 — idExterno novo não é duplicata");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 3 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 3 — exceção inesperada: " + e.getMessage());
        }

        // Teste 4 — reunião com idExterno existente é marcada como duplicata
        try {
            Reuniao original = new Reuniao();
            original.setId(1L);
            original.setIdExterno("MTG-999");
            original.setDuplicada(false);

            DuplicidadeService service = new DuplicidadeService(new ReuniaoRepositoryComOriginal(original));

            Reuniao nova = new Reuniao();
            nova.setId(2L);
            nova.setIdExterno("MTG-999");

            boolean resultado = service.verificarEMarcar(nova);

            if (!resultado) throw new AssertionError("Esperava true (duplicata detectada), mas retornou false");
            if (!Boolean.TRUE.equals(nova.getDuplicada())) throw new AssertionError("Reunião deveria ter sido marcada como duplicada");
            passou++;
            System.out.println("[OK] Teste 4 — idExterno duplicado é corretamente detectado e marcado");
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] Teste 4 — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] Teste 4 — exceção inesperada: " + e.getMessage());
        }

        System.out.println("\nResultado: " + passou + " passou(ram) | " + falhou + " falhou(ram)");
    }
}