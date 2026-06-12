package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.ReuniaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Detecta e marca duplicatas entre lotes de importacao.
 * Uma reuniao e duplicata se ja existe outra com o mesmo idExterno
 * nao marcada como duplicada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicidadeService {

    private final ReuniaoRepository reuniaoRepository;

    /**
     * Verifica se a reuniao e duplicata.
     * Se for, marca a reuniao atual como duplicada e retorna true.
     * Se nao for, retorna false (a reuniao e valida).
     */
    public boolean verificarEMarcar(Reuniao reuniao) {
        String idExterno = reuniao.getIdExterno();
        if (idExterno == null || idExterno.isBlank()) return false;

        Optional<Reuniao> original = reuniaoRepository.findTopByIdExternoAndDuplicadaFalse(idExterno);
        if (original.isEmpty()) return false;

        Reuniao orig = original.get();
        // Nao marcar como duplicada se for a mesma entidade
        if (orig.getId() != null && orig.getId().equals(reuniao.getId())) return false;

        reuniao.setDuplicada(true);
        reuniao.setMotivoDuplicidade("ID_MEETING " + idExterno + " ja existe no banco (origem: "
            + (orig.getNomeArquivoOrigem() != null ? orig.getNomeArquivoOrigem() : "desconhecido")
            + ", lote: " + (orig.getLoteImportacao() != null ? orig.getLoteImportacao().getCodigoLote() : "-") + ")");
        reuniao.setIdReuniaoOriginalDuplicada(orig.getId());

        log.debug("Duplicata detectada: idExterno={} | original reuniao #{}", idExterno, orig.getId());
        return true;
    }
}
