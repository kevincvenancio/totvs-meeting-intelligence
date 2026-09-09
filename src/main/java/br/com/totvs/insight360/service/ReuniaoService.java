package br.com.totvs.insight360.service;

import br.com.totvs.insight360.dao.ComentarioReuniaoDao;
import br.com.totvs.insight360.dao.PlanoAcaoDao;
import br.com.totvs.insight360.dao.ReuniaoDao;
import br.com.totvs.insight360.dto.filtro.FiltroPlanoAcao;
import br.com.totvs.insight360.dto.filtro.FiltroReuniao;
import br.com.totvs.insight360.dto.mapper.ReuniaoMapper;
import br.com.totvs.insight360.dto.requisicao.ReuniaoRequisicao;
import br.com.totvs.insight360.exception.ConflitoDeDadosException;
import br.com.totvs.insight360.exception.RecursoNaoEncontradoException;
import br.com.totvs.insight360.exception.RegraNegocioException;
import br.com.totvs.insight360.model.Cliente;
import br.com.totvs.insight360.model.FeedbackReuniao;
import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.FeedbackReuniaoRepository;
import br.com.totvs.insight360.repository.InsightRepository;
import br.com.totvs.insight360.repository.ReuniaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de domínio para Reunião.
 * <p>
 * Reúne as consultas usadas pela CLI e pelos relatórios em PDF e o CRUD completo
 * consumido pela API REST, incluindo o cadastro manual de reuniões (que dispara a
 * mesma análise automática aplicada às transcrições importadas por CSV).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReuniaoService {

    private static final String RECURSO = "Reunião";

    private final ReuniaoRepository reuniaoRepository;
    private final InsightRepository insightRepository;
    private final FeedbackReuniaoRepository feedbackRepository;
    private final ReuniaoDao reuniaoDao;
    private final PlanoAcaoDao planoAcaoDao;
    private final ComentarioReuniaoDao comentarioDao;
    private final ClienteService clienteService;
    private final AnaliseTranscricaoService analiseService;
    private final FeedbackReuniaoService feedbackService;

    // ── CRUD ──────────────────────────────────────────────────────────

    /**
     * Cadastra manualmente uma reunião e executa a análise automática da transcrição.
     *
     * @throws ConflitoDeDadosException quando o identificador externo já está em uso
     */
    @Transactional
    public Reuniao cadastrar(ReuniaoRequisicao requisicao) {
        validarIdExternoDisponivel(requisicao.idExterno(), null);

        Reuniao reuniao = new Reuniao();
        ReuniaoMapper.aplicar(requisicao, reuniao);
        reuniao.setAnalisada(false);
        reuniao.setDuplicada(false);
        reuniao.setDataImportacao(LocalDateTime.now());
        reuniao.setNomeArquivoOrigem("Cadastro manual via API");
        aplicarVinculoDeCliente(reuniao, requisicao.clienteId());

        Reuniao salva = reuniaoRepository.save(reuniao);
        analisar(salva);
        log.info("Reuniao #{} cadastrada manualmente para o cliente '{}'", salva.getId(), salva.getCliente());
        return salva;
    }

    /**
     * Atualiza uma reunião. Quando a transcrição muda, a análise automática é
     * refeita para manter os campos derivados coerentes.
     */
    @Transactional
    public Reuniao atualizar(Long id, ReuniaoRequisicao requisicao) {
        Reuniao reuniao = buscarObrigatoria(id);
        validarIdExternoDisponivel(requisicao.idExterno(), id);

        boolean transcricaoAlterada = !requisicao.transcricao().equals(reuniao.getTranscricaoOriginal());
        ReuniaoMapper.aplicar(requisicao, reuniao);
        aplicarVinculoDeCliente(reuniao, requisicao.clienteId());

        Reuniao salva = reuniaoRepository.save(reuniao);
        if (transcricaoAlterada) {
            analisar(salva);
            log.info("Reuniao #{} reanalisada apos alteracao da transcricao", id);
        }
        return salva;
    }

    /**
     * Remove a reunião e todo o conteúdo derivado dela: insights, feedback
     * educativo, comentários e planos de ação.
     */
    @Transactional
    public void remover(Long id) {
        Reuniao reuniao = buscarObrigatoria(id);
        int planosRemovidos = planoAcaoDao.removerPorReuniao(id);
        int comentariosRemovidos = comentarioDao.removerPorReuniao(id);
        insightRepository.deleteByReuniaoId(id);
        feedbackRepository.findByReuniaoId(id).ifPresent(feedbackRepository::delete);
        reuniaoRepository.delete(reuniao);
        log.info("Reuniao #{} removida ({} plano(s), {} comentario(s))",
                id, planosRemovidos, comentariosRemovidos);
    }

    /** Executa novamente a análise automática da transcrição já gravada. */
    @Transactional
    public Reuniao reanalisar(Long id) {
        Reuniao reuniao = buscarObrigatoria(id);
        String transcricao = reuniao.getTranscricaoTratada() != null
                ? reuniao.getTranscricaoTratada() : reuniao.getTranscricaoOriginal();
        if (transcricao == null || transcricao.isBlank()) {
            throw new RegraNegocioException(
                    "A reunião #" + id + " não possui transcrição e não pode ser analisada.");
        }
        analisar(reuniao);
        return reuniao;
    }

    // ── Vínculo com cliente cadastrado ────────────────────────────────

    /** Vincula a reunião a um cliente da carteira. */
    @Transactional
    public Reuniao vincularCliente(Long reuniaoId, Long clienteId) {
        Reuniao reuniao = buscarObrigatoria(reuniaoId);
        Cliente cliente = clienteService.buscarPorId(clienteId);
        reuniao.setClienteVinculado(cliente);
        Reuniao salva = reuniaoRepository.save(reuniao);
        log.info("Reuniao #{} vinculada ao cliente #{}", reuniaoId, clienteId);
        return salva;
    }

    /** Desfaz o vínculo entre a reunião e o cliente cadastrado. */
    @Transactional
    public Reuniao desvincularCliente(Long reuniaoId) {
        Reuniao reuniao = buscarObrigatoria(reuniaoId);
        reuniao.setClienteVinculado(null);
        return reuniaoRepository.save(reuniao);
    }

    // ── Consultas da API ──────────────────────────────────────────────

    /** Pesquisa paginada aplicando os critérios informados. */
    public List<Reuniao> pesquisar(FiltroReuniao filtro, int pagina, int tamanho) {
        return reuniaoDao.pesquisar(filtro, pagina, tamanho);
    }

    /** Total de registros correspondentes ao filtro. */
    public long contar(FiltroReuniao filtro) {
        return reuniaoDao.contar(filtro);
    }

    /**
     * Busca a reunião com lote e cliente já carregados.
     *
     * @throws RecursoNaoEncontradoException quando o id não existe
     */
    public Reuniao buscarDetalhada(Long id) {
        return reuniaoDao.buscarDetalhada(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de(RECURSO, id));
    }

    /** Insights extraídos de uma reunião existente. */
    public List<Insight> listarInsights(Long reuniaoId) {
        garantirExistencia(reuniaoId);
        return insightRepository.findByReuniaoId(reuniaoId);
    }

    /**
     * Feedback educativo de uma reunião existente.
     *
     * @throws RecursoNaoEncontradoException quando a reunião não gerou feedback
     */
    public FeedbackReuniao buscarFeedback(Long reuniaoId) {
        garantirExistencia(reuniaoId);
        return feedbackRepository.findByReuniaoId(reuniaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "A reunião #" + reuniaoId + " ainda não possui feedback educativo."));
    }

    /** Quantidade de insights de uma reunião. */
    public long contarInsights(Long reuniaoId) {
        return insightRepository.findByReuniaoId(reuniaoId).size();
    }

    /** Quantidade de planos de ação de uma reunião. */
    public long contarPlanosAcao(Long reuniaoId) {
        return planoAcaoDao.contar(new FiltroPlanoAcao(null, reuniaoId, null, null, null,
                null, null, null, null));
    }

    /** Quantidade de comentários de uma reunião. */
    public long contarComentarios(Long reuniaoId) {
        return comentarioDao.contarPorReuniao(reuniaoId);
    }

    /** Garante que a reunião existe antes de operações que apenas a referenciam. */
    public void garantirExistencia(Long id) {
        if (!reuniaoDao.existePorId(id)) {
            throw RecursoNaoEncontradoException.de(RECURSO, id);
        }
    }

    // ── Consultas usadas pela CLI e pelos relatórios ──────────────────

    public List<Reuniao> listarAnalisadas() {
        return reuniaoRepository.findByAnalisadaTrue();
    }

    public List<Reuniao> listarTodasOrdenadas() {
        return reuniaoRepository.findAllAnalisadasOrderByScore();
    }

    public List<Reuniao> buscarPorCliente(String termo) {
        return reuniaoRepository.findByClienteContainingIgnoreCase(termo);
    }

    public List<Reuniao> filtrarPorStatus(String status) {
        return reuniaoRepository.findByStatusCompletude(status);
    }

    public List<Reuniao> filtrarPorSentimento(String sentimento) {
        return reuniaoRepository.findBySentimento(sentimento);
    }

    public List<Reuniao> filtrarPorPrioridade(String prioridade) {
        return reuniaoRepository.findByPrioridade(prioridade);
    }

    public List<Reuniao> listarChurnAlto() {
        return reuniaoRepository.findByAltoRiscoChurn();
    }

    public Optional<Reuniao> buscarPorId(Long id) {
        return reuniaoRepository.findById(id);
    }

    public long contarAnalisadas() {
        return reuniaoRepository.countAnalisadas();
    }

    public long contar() {
        return reuniaoRepository.count();
    }

    public long contarPorLote(Long loteId) {
        return reuniaoRepository.countByLoteId(loteId);
    }

    public List<Reuniao> listarPorLote(Long loteId) {
        return reuniaoRepository.findByLoteId(loteId);
    }

    public List<Insight> listarInsightsPorReuniao(Long reuniaoId) {
        return insightRepository.findByReuniaoId(reuniaoId);
    }

    public Optional<FeedbackReuniao> buscarFeedbackPorReuniao(Long reuniaoId) {
        return feedbackRepository.findByReuniaoId(reuniaoId);
    }

    public List<FeedbackReuniao> listarFeedbacksOrdenadosPorCriticidade() {
        return feedbackRepository.findAllByOrderByNivelCriticidadeDesc();
    }

    // ── Apoio ─────────────────────────────────────────────────────────

    private Reuniao buscarObrigatoria(Long id) {
        return reuniaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de(RECURSO, id));
    }

    /** Executa a análise da transcrição e regenera o feedback educativo. */
    private void analisar(Reuniao reuniao) {
        analiseService.analisarReuniao(reuniao);
        feedbackService.gerarFeedback(reuniao);
    }

    private void aplicarVinculoDeCliente(Reuniao reuniao, Long clienteId) {
        if (clienteId == null) {
            return;
        }
        reuniao.setClienteVinculado(clienteService.buscarPorId(clienteId));
    }

    private void validarIdExternoDisponivel(String idExterno, Long idIgnorado) {
        if (idExterno == null || idExterno.isBlank()) {
            return;
        }
        reuniaoRepository.findTopByIdExternoAndDuplicadaFalse(idExterno.trim())
                .filter(existente -> !existente.getId().equals(idIgnorado))
                .ifPresent(existente -> {
                    throw new ConflitoDeDadosException("Já existe a reunião #" + existente.getId()
                            + " com o identificador externo " + idExterno + ".");
                });
    }
}
