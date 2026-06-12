package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.ImportacaoLote;
import br.com.totvs.insight360.repository.ImportacaoLoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Gerencia os lotes de importacao de CSV.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacaoLoteService {

    private final ImportacaoLoteRepository loteRepository;

    /**
     * Cria um lote para o arquivo CSV informado.
     * Verifica previamente se o mesmo arquivo já foi importado (via hash MD5).
     *
     * @param arquivo arquivo CSV a ser importado
     * @return lote criado
     */
    @Transactional
    public ImportacaoLote criarLote(File arquivo) {
        String hash = calcularHash(arquivo);

        // Verificar reimportacao do mesmo arquivo
        loteRepository.findByHashArquivo(hash).ifPresent(l ->
                log.warn("Arquivo '{}' ja foi importado como {} em {}",
                        arquivo.getName(), l.getCodigoLote(), l.getDataHoraImportacao()));

        long total = loteRepository.count();
        String codigo = String.format("Lote %03d", total + 1);

        ImportacaoLote lote = new ImportacaoLote();
        lote.setCodigoLote(codigo);
        lote.setNomeArquivoOriginal(arquivo.getName());
        lote.setDataHoraImportacao(LocalDateTime.now());
        lote.setStatusProcessamento("PROCESSANDO");
        lote.setHashArquivo(hash);
        lote.setTotalRegistrosBrutos(0);
        lote.setTotalReunioesValidas(0);
        lote.setTotalReunioesIncompletas(0);
        lote.setTotalReunioesDuplicadas(0);
        lote.setTotalReunioesComErro(0);

        ImportacaoLote salvo = loteRepository.save(lote);
        log.info("Lote criado: {} para arquivo '{}'", codigo, arquivo.getName());
        return salvo;
    }

    /**
     * Finaliza o lote com os totais do processamento.
     */
    @Transactional
    public void finalizarLote(ImportacaoLote lote, int brutos, int validas,
                              int incompletas, int duplicadas, int erros,
                              String observacoes) {
        lote.setTotalRegistrosBrutos(brutos);
        lote.setTotalReunioesValidas(validas);
        lote.setTotalReunioesIncompletas(incompletas);
        lote.setTotalReunioesDuplicadas(duplicadas);
        lote.setTotalReunioesComErro(erros);
        lote.setStatusProcessamento(erros > 0 && validas == 0 ? "ERRO" : "CONCLUIDO");
        lote.setObservacoes(observacoes);
        loteRepository.save(lote);
        log.info("Lote {} finalizado: validas={} duplicadas={} erros={}",
                lote.getCodigoLote(), validas, duplicadas, erros);
    }

    /**
     * Lista todos os lotes em ordem decrescente de importacao.
     */
    public List<ImportacaoLote> listarTodos() {
        return loteRepository.findAllByOrderByDataHoraImportacaoDesc();
    }

    /**
     * Busca o lote mais recente.
     */
    public Optional<ImportacaoLote> ultimoLote() {
        return loteRepository.findTopByOrderByDataHoraImportacaoDesc();
    }

    /**
     * Busca lote pelo codigo (ex: "Lote 003").
     */
    public Optional<ImportacaoLote> buscarPorCodigo(String codigo) {
        return loteRepository.findByCodigoLote(codigo);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String calcularHash(File arquivo) {
        try {
            byte[] bytes = Files.readAllBytes(arquivo.toPath());
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException | java.io.IOException e) {
            log.warn("Erro ao calcular hash: {}", e.getMessage());
            return arquivo.getName() + "_" + arquivo.length();
        }
    }
}