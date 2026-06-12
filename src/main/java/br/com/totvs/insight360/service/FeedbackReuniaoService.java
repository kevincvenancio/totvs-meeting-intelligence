package br.com.totvs.insight360.service;

import br.com.totvs.insight360.model.FeedbackReuniao;
import br.com.totvs.insight360.model.NivelCriticidade;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.repository.FeedbackReuniaoRepository;
import br.com.totvs.insight360.util.TextoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Serviço responsavel por gerar ‘feedback’ educativo para cada reuniao analisada.
 */
@Service
@RequiredArgsConstructor
public class FeedbackReuniaoService {

    private final FeedbackReuniaoRepository feedbackRepository;

    /**
     * Gera e salva o feedback educativo para uma reuniao.
     * Remove feedback anterior se existir.
     *
     * @param reuniao reuniao ja analisada
     */
    @Transactional
    public void gerarFeedback(Reuniao reuniao) {
        feedbackRepository.findByReuniaoId(reuniao.getId())
                .ifPresent(feedbackRepository::delete);

        FeedbackReuniao fb = new FeedbackReuniao();
        fb.setReuniao(reuniao);

        String texto = reuniao.getTranscricaoTratada() != null
                ? reuniao.getTranscricaoTratada().toLowerCase()
                : (reuniao.getTranscricaoOriginal() != null
                ? reuniao.getTranscricaoOriginal().toLowerCase() : "");

        List<String> problemas  = new ArrayList<>();
        List<String> sinais     = new ArrayList<>();
        List<String> perguntas  = new ArrayList<>();
        List<String> acoes      = new ArrayList<>();
        String categoriaFb = "Analise Geral";
        String criticidade = "BAIXA";

        // ── Padrao 1: Processo manual / Retrabalho ───────────────────
        if (TextoUtils.contemQualquer(texto,
                "processo manual", "planilha excel", "retrabalho", "folha manual")) {
            problemas.add("Processo manual ou ineficiencia operacional identificada na conversa");
            categoriaFb = "Processo manual / Retrabalho";
            criticidade = "ALTA";
            sinais.add("Mencao a processos manuais, planilhas ou retrabalho");
            sinais.add("Expressoes como 'sofrendo', 'demora', 'manual' durante a conversa");
            perguntas.add("'Qual processo consome mais tempo da sua equipe hoje?'");
            perguntas.add("'Esse retrabalho gera custo, atraso ou risco operacional?'");
            perguntas.add("'Quantas pessoas participam desse processo manual por semana?'");
            acoes.add("Mapear o processo atual e calcular tempo/custo perdido");
            acoes.add("Apresentar demonstracao de automacao com ganho de produtividade mensuravel");
        }

        // ── Padrao 2: Concorrente mencionado ─────────────────────────
        if (TextoUtils.contemQualquer(texto,
                "senior sistemas", "sap s/4", "oracle erp", "linx sistemas",
                "sankhya", "salesforce crm", "dynamics 365", "omie erp", "avaliando outra")) {
            problemas.add("Risco competitivo: cliente avaliando ou mencionou solucao concorrente");
            categoriaFb = problemas.size() > 1 ? "Multiplos" : "Pressao competitiva";
            criticidade = "ALTA";
            sinais.add("Mencao direta a concorrente pelo nome");
            sinais.add("Comentarios sobre 'demo', 'proposta' ou 'avaliacao' de outra solucao");
            perguntas.add("'Quais solucoes voces estao avaliando atualmente?'");
            perguntas.add("'O que chamou a atencao nessa alternativa?'");
            perguntas.add("'Qual o criterio principal que vai guiar a decisao?'");
            acoes.add("Preparar material comparativo destacando diferenciais exclusivos da TOTVS");
            acoes.add("Envolver especialista comercial para aprofundar o processo de decisao");
        }

        // ── Padrao 3: Orcamento ──────────────────────────────────────
        if (TextoUtils.contemQualquer(texto,
                "orcamento", "budget", "r$", "investimento", "roi", "retorno sobre")) {
            problemas.add("Oportunidade com sinal de orcamento ou decisao financeira");
            if (!"ALTA".equals(criticidade)) criticidade = "MEDIA";
            sinais.add("Mencao a valores, orcamento, ROI ou aprovacao financeira");
            sinais.add("Presenca do CFO ou gestor financeiro na conversa");
            perguntas.add("'Existe orcamento reservado para esse projeto neste semestre?'");
            perguntas.add("'Quem aprova esse tipo de investimento na empresa?'");
            perguntas.add("'Qual o ROI esperado para justificar esse investimento?'");
            acoes.add("Preparar proposta com calculo de ROI e payback");
            acoes.add("Solicitar reuniao com decisor financeiro para apresentacao da proposta");
        }

        // ── Padrao 4: Decisor estrategico ────────────────────────────
        if (TextoUtils.contemQualquer(texto,
                "cfo", "ceo", "diretoria", "comite de aprovacao", "decisao final", "presidente")) {
            problemas.add("Presenca de decisor estrategico ou necessidade de validacao executiva");
            sinais.add("Mencao ao CFO, CEO, diretoria ou comite de aprovacao");
            perguntas.add("'Quem mais participa dessa decisao alem de voce?'");
            perguntas.add("'Qual o processo de aprovacao para esse tipo de investimento?'");
            acoes.add("Preparar apresentacao executiva com linguagem de negocios (ROI, payback, risco)");
            acoes.add("Solicitar acesso direto ao decisor financeiro");
        }

        // ── Padrao 5: Risco de churn / insatisfacao ──────────────────
        if (TextoUtils.contemQualquer(texto,
                "cancelar contrato", "nao renovar", "trocar de fornecedor",
                "insatisfeito com", "sair da totvs")) {
            problemas.add("Sinal de risco de cancelamento ou nao renovacao");
            categoriaFb = "Risco de churn / Insatisfacao";
            criticidade = "ALTA";
            sinais.add("Expressoes de insatisfacao direta ou intencao de cancelamento");
            perguntas.add("'Qual seria o principal motivo que levaria a nao renovar conosco?'");
            perguntas.add("'O que precisaria mudar para voce continuar parceiro TOTVS?'");
            acoes.add("Acionar imediatamente o time de Customer Success");
            acoes.add("Criar plano de recuperacao com metas e acompanhamento semanal");
            acoes.add("Envolver gestor senior para demonstrar comprometimento TOTVS");
        }

        // ── Padrao 6: Falta de integracao ────────────────────────────
        if (TextoUtils.contemQualquer(texto,
                "nao integra", "falta integracao", "sistemas separados",
                "dados espalhados", "lancar em dois sistemas")) {
            problemas.add("Necessidade de integracao entre sistemas identificada");
            sinais.add("Reclamacao sobre sistemas que nao se comunicam");
            sinais.add("Mencao a lancamentos duplicados ou dados inconsistentes");
            perguntas.add("'Quais sistemas precisam se comunicar hoje e nao conseguem?'");
            perguntas.add("'Quanto tempo a equipe gasta fazendo lancamentos duplicados?'");
            acoes.add("Mapear o ecossistema de sistemas do cliente");
            acoes.add("Propor solucao de integracao via TOTVS Fluig ou Carol");
        }

        // ── Fallback: sem padrao identificado ────────────────────────
        if (problemas.isEmpty()) {
            problemas.add("Reuniao com baixa densidade de informacoes comerciais identificadas");
            sinais.add("Conversa generica sem aprofundamento em dores, orcamento ou decisores");
            perguntas.add("'Quais sao os 3 maiores desafios operacionais que voce enfrenta hoje?'");
            perguntas.add("'Se voce pudesse resolver um problema com tecnologia agora, qual seria?'");
            acoes.add("Realizar reuniao de descoberta aprofundada antes da proxima etapa");
        }

        fb.setProblemaIdentificado("• " + String.join("\n• ", problemas));
        fb.setCategoriaProblema(categoriaFb);
        fb.setSinaisNaConversa("• " + String.join("\n• ", sinais));
        fb.setMotivoNaoIdentificadoAntes(gerarMotivo(texto, reuniao));
        fb.setComoIdentificarAntes(gerarComoIdentificar(categoriaFb));
        fb.setPerguntasRecomendadas("• " + String.join("\n• ", perguntas));
        fb.setAcaoDeMelhoria("• " + String.join("\n• ", acoes));
        fb.setNivelCriticidade(NivelCriticidade.valueOf(criticidade));
        fb.setMensagemEducativa(gerarMensagemEducativa(reuniao, criticidade));

        feedbackRepository.save(fb);
    }

    // ── Helpers de geracao de texto ───────────────────────────────────

    private String gerarMotivo(String texto, Reuniao reuniao) {
        List<String> motivos = new ArrayList<>();
        if (reuniao.getScoreComercial() != null && reuniao.getScoreComercial() < 40) {
            motivos.add("Score comercial baixo indica que oportunidades passaram despercebidas");
        }
        motivos.add("Sinais de oportunidade aparecem de forma indireta — nao como solicitacao explicita");
        motivos.add("A urgencia do momento pode levar o consultor a focar na apresentacao em vez de explorar dores");
        if (TextoUtils.contemQualquer(texto, "senior", "sap", "oracle", "linx")) {
            motivos.add("Mencoes a concorrentes foram feitas casualmente e podem ter passado despercebidas");
        }
        return String.join(". ", motivos) + ".";
    }

    private String gerarComoIdentificar(String categoriaFb) {
        return switch (categoriaFb) {
            case "Processo manual / Retrabalho" ->
                    "Quando o cliente usar 'manual', 'planilha', 'demora' ou 'retrabalho', " +
                            "pausar a apresentacao e perguntar sobre o impacto real: custo, tempo e quem e afetado.";
            case "Pressao competitiva" ->
                    "Qualquer mencao a nomes de concorrentes, mesmo casual, deve ser explorada imediatamente. " +
                            "Perguntar o que chamou atencao na alternativa revela os criterios de decisao do cliente.";
            case "Risco de churn / Insatisfacao" ->
                    "Expressoes de frustracao, mesmo sutis, devem ser aprofundadas. " +
                            "A pergunta 'O que precisaria mudar para voce continuar?' pode abrir conversa decisiva.";
            case "Decisao financeira / Orcamento" ->
                    "Sempre que houver mencao a valores, ROI ou aprovacao, mapear imediatamente quem decide. " +
                            "Orcamento identificado e oportunidade qualificada.";
            default ->
                    "Praticar escuta ativa e fazer ao menos 3 perguntas abertas sobre dores, urgencia e " +
                            "decisores antes de apresentar qualquer solucao.";
        };
    }

    private String gerarMensagemEducativa(Reuniao reuniao, String criticidade) {
        String nome = reuniao.getCliente() != null ? reuniao.getCliente() : "nesta reuniao";
        StringBuilder msg = new StringBuilder();

        msg.append("Aprendizado desta reuniao com ").append(nome).append(":\n\n");

        if ("ALTA".equals(criticidade)) {
            msg.append("Esta reuniao apresenta sinais criticos que merecem acao imediata.\n\n");
        }

        msg.append("As oportunidades comerciais muitas vezes se escondem em frases indiretas. ")
                .append("Palavras como 'manual', 'sofrendo', 'concorrente', 'orcamento' e 'ROI' ")
                .append("sao gatilhos que devem ativar perguntas de aprofundamento.\n\n");

        msg.append("Para as proximas reunioes:\n");
        msg.append("• Ouca mais do que fala — as dores do cliente sao o caminho para a venda\n");
        msg.append("• Mapeie sempre quem decide e qual o processo de aprovacao\n");
        msg.append("• Orcamento identificado = oportunidade qualificada. Nao deixe passar\n");
        msg.append("• Concorrente mencionado = urgencia para reforcar diferenciais TOTVS\n");
        msg.append("• Insatisfacao e oportunidade de fidelizacao, nao apenas um problema\n\n");
        msg.append("\"O ouro invisivel esta nas palavras que o cliente diz sem perceber que esta pedindo ajuda.\"");

        return msg.toString();
    }

    public Optional<FeedbackReuniao> buscarPorReuniao(Long reuniaoId) {
        return feedbackRepository.findByReuniaoId(reuniaoId);
    }
}