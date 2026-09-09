package br.com.totvs.insight360.service;

import br.com.totvs.insight360.dao.PlanoAcaoDao;
import br.com.totvs.insight360.dao.ReuniaoDao;
import br.com.totvs.insight360.dto.filtro.FiltroPlanoAcao;
import br.com.totvs.insight360.dto.requisicao.AtualizacaoPlanoAcaoRequisicao;
import br.com.totvs.insight360.dto.requisicao.PlanoAcaoRequisicao;
import br.com.totvs.insight360.dto.requisicao.TransicaoStatusRequisicao;
import br.com.totvs.insight360.exception.RecursoNaoEncontradoException;
import br.com.totvs.insight360.exception.RegraNegocioException;
import br.com.totvs.insight360.model.PlanoAcao;
import br.com.totvs.insight360.model.PrioridadePlanoAcao;
import br.com.totvs.insight360.model.Reuniao;
import br.com.totvs.insight360.model.StatusPlanoAcao;
import br.com.totvs.insight360.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regras de negócio de {@link PlanoAcao} — o acompanhamento das ações comerciais
 * criadas a partir das reuniões analisadas.
 * <p>
 * O ciclo de vida do plano é controlado por {@link StatusPlanoAcao}: o serviço
 * apenas consulta o enum para saber se a transição pedida é válida, o que mantém
 * a regra em um único lugar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanoAcaoService {

    private static final String RECURSO = "Plano de ação";

    private final PlanoAcaoDao planoAcaoDao;
    private final ReuniaoDao reuniaoDao;
    private final UsuarioService usuarioService;

    // ── CRUD ──────────────────────────────────────────────────────────

    /**
     * Cria um plano de ação vinculado a uma reunião.
     *
     * @throws RecursoNaoEncontradoException quando a reunião ou o responsável não existem
     * @throws RegraNegocioException         quando a reunião é duplicada ou o prazo já venceu
     */
    @Transactional
    public PlanoAcao criar(PlanoAcaoRequisicao requisicao) {
        Reuniao reuniao = buscarReuniao(requisicao.reuniaoId());
        if (Boolean.TRUE.equals(reuniao.getDuplicada())) {
            throw new RegraNegocioException(
                    "A reunião #" + reuniao.getId() + " está marcada como duplicada e não aceita planos de ação.");
        }
        Usuario responsavel = usuarioService.buscarAtivoPorId(requisicao.responsavelId());
        validarPrazo(requisicao.prazo());

        PlanoAcao plano = new PlanoAcao();
        plano.setTitulo(requisicao.titulo().trim());
        plano.setDescricao(requisicao.descricao());
        plano.setReuniao(reuniao);
        plano.setResponsavel(responsavel);
        plano.setPrioridade(requisicao.prioridade() != null
                ? requisicao.prioridade() : PrioridadePlanoAcao.MEDIA);
        plano.setStatus(StatusPlanoAcao.PENDENTE);
        plano.setPrazo(requisicao.prazo());

        PlanoAcao salvo = planoAcaoDao.inserir(plano);
        log.info("Plano de acao #{} criado para a reuniao #{}", salvo.getId(), reuniao.getId());
        return salvo;
    }

    /**
     * Atualiza os dados de um plano ainda em aberto.
     *
     * @throws RegraNegocioException quando o plano já foi concluído ou cancelado
     */
    @Transactional
    public PlanoAcao atualizar(Long id, AtualizacaoPlanoAcaoRequisicao requisicao) {
        PlanoAcao plano = buscarPorId(id);
        garantirPlanoEmAberto(plano);

        Usuario responsavel = usuarioService.buscarAtivoPorId(requisicao.responsavelId());
        if (!requisicao.prazo().equals(plano.getPrazo())) {
            validarPrazo(requisicao.prazo());
        }

        plano.setTitulo(requisicao.titulo().trim());
        plano.setDescricao(requisicao.descricao());
        plano.setResponsavel(responsavel);
        plano.setPrioridade(requisicao.prioridade());
        plano.setPrazo(requisicao.prazo());
        return planoAcaoDao.atualizar(plano);
    }

    /**
     * Move o plano para outro status respeitando as transições do ciclo de vida.
     *
     * @throws RegraNegocioException quando a transição não é permitida ou falta o resultado
     */
    @Transactional
    public PlanoAcao transicionarStatus(Long id, TransicaoStatusRequisicao requisicao) {
        PlanoAcao plano = buscarPorId(id);
        StatusPlanoAcao statusAtual = plano.getStatus();
        StatusPlanoAcao novoStatus = requisicao.novoStatus();

        if (!statusAtual.podeTransicionarPara(novoStatus)) {
            throw new RegraNegocioException(descreverTransicaoInvalida(statusAtual, novoStatus));
        }
        if (novoStatus.isFinal() && (requisicao.resultado() == null || requisicao.resultado().isBlank())) {
            throw new RegraNegocioException(
                    "Informe o resultado ao mover o plano para " + novoStatus.getDescricao() + ".");
        }

        plano.setStatus(novoStatus);
        plano.setResultado(requisicao.resultado());
        plano.setDataConclusao(novoStatus.isFinal() ? LocalDateTime.now() : null);

        PlanoAcao atualizado = planoAcaoDao.atualizar(plano);
        log.info("Plano de acao #{}: {} -> {}", id, statusAtual, novoStatus);
        return atualizado;
    }

    /** Remove um plano de ação. */
    @Transactional
    public void remover(Long id) {
        PlanoAcao plano = buscarPorId(id);
        planoAcaoDao.remover(plano);
        log.info("Plano de acao #{} removido", id);
    }

    /** Busca o plano com reunião e responsável carregados. */
    public PlanoAcao buscarPorId(Long id) {
        return planoAcaoDao.buscarDetalhado(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de(RECURSO, id));
    }

    // ── Consultas ─────────────────────────────────────────────────────

    /** Pesquisa paginada aplicando os critérios informados. */
    public List<PlanoAcao> pesquisar(FiltroPlanoAcao filtro, int pagina, int tamanho) {
        return planoAcaoDao.pesquisar(filtro, pagina, tamanho);
    }

    /** Total de registros correspondentes ao filtro. */
    public long contar(FiltroPlanoAcao filtro) {
        return planoAcaoDao.contar(filtro);
    }

    /** Planos criados a partir de uma reunião. */
    public List<PlanoAcao> listarPorReuniao(Long reuniaoId) {
        return planoAcaoDao.listarPorReuniao(reuniaoId);
    }

    /** Planos atribuídos a um responsável. */
    public List<PlanoAcao> listarPorResponsavel(Long responsavelId) {
        usuarioService.buscarPorId(responsavelId);
        return planoAcaoDao.listarPorResponsavel(responsavelId);
    }

    /** Planos em aberto com prazo vencido. */
    public List<PlanoAcao> listarAtrasados() {
        return planoAcaoDao.listarAtrasados();
    }

    /** Quantidade de planos por status, com a chave já no formato exibido na API. */
    public Map<String, Long> contarPorStatus() {
        Map<String, Long> totais = new LinkedHashMap<>();
        planoAcaoDao.contarPorStatus().forEach((status, total) -> totais.put(status.name(), total));
        return totais;
    }

    /** Quantidade de planos em aberto com prazo vencido. */
    public long contarAtrasados() {
        return planoAcaoDao.contarAtrasados();
    }

    /** Total de planos cadastrados. */
    public long contar() {
        return planoAcaoDao.contar();
    }

    // ── Validações internas ───────────────────────────────────────────

    private Reuniao buscarReuniao(Long reuniaoId) {
        return reuniaoDao.buscarPorId(reuniaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Reunião", reuniaoId));
    }

    private void validarPrazo(LocalDate prazo) {
        if (prazo.isBefore(LocalDate.now())) {
            throw new RegraNegocioException("O prazo não pode ser anterior à data de hoje.");
        }
    }

    private void garantirPlanoEmAberto(PlanoAcao plano) {
        if (plano.getStatus() != null && plano.getStatus().isFinal()) {
            throw new RegraNegocioException("O plano de ação está " + plano.getStatus().getDescricao()
                    + " e não pode mais ser editado.");
        }
    }

    private String descreverTransicaoInvalida(StatusPlanoAcao atual, StatusPlanoAcao novo) {
        if (atual == novo) {
            return "O plano de ação já está com o status " + atual.getDescricao() + ".";
        }
        if (atual.transicoesPermitidas().isEmpty()) {
            return "O plano de ação está " + atual.getDescricao() + " e não admite novas mudanças de status.";
        }
        String permitidos = atual.transicoesPermitidas().stream()
                .map(StatusPlanoAcao::name)
                .sorted()
                .collect(Collectors.joining(", "));
        return "Transição inválida de " + atual.name() + " para " + novo.name()
                + ". Status permitidos: " + permitidos + ".";
    }
}
