package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.FeedbackReuniao;
import br.com.totvs.insight360.model.Insight;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.FeedbackReuniaoRepository;
import br.com.totvs.insight360.repository.InsightRepository;
import br.com.totvs.insight360.repository.ReuniaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Serviço de domínio para Reunião..
 */
@Service
@RequiredArgsConstructor
public class ReuniaoService {

    private final ReuniaoRepository reuniaoRepository;
    private final InsightRepository insightRepository;
    private final FeedbackReuniaoRepository feedbackRepository;

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
}