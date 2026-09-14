package br.com.totvs.hermes.service;

import br.com.totvs.hermes.dao.ClienteDao;
import br.com.totvs.hermes.dao.ReuniaoDao;
import br.com.totvs.hermes.dto.mapper.ClienteMapper;
import br.com.totvs.hermes.dto.requisicao.ClienteRequisicao;
import br.com.totvs.hermes.exception.ConflitoDeDadosException;
import br.com.totvs.hermes.exception.RecursoNaoEncontradoException;
import br.com.totvs.hermes.exception.RegraNegocioException;
import br.com.totvs.hermes.model.Cliente;
import br.com.totvs.hermes.model.Reuniao;
import br.com.totvs.hermes.model.StatusCliente;
import br.com.totvs.hermes.util.ValidadorCnpj;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Regras de negócio de {@link Cliente}: cadastro, edição, exclusão e consulta da
 * carteira, incluindo a validação de CNPJ e o vínculo com as reuniões.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClienteService {

    private static final String RECURSO = "Cliente";

    private final ClienteDao clienteDao;
    private final ReuniaoDao reuniaoDao;

    // ── CRUD ──────────────────────────────────────────────────────────

    /**
     * Cadastra um cliente validando o CNPJ e a unicidade.
     *
     * @throws RegraNegocioException    quando o CNPJ é inválido
     * @throws ConflitoDeDadosException quando o CNPJ já está cadastrado
     */
    @Transactional
    public Cliente cadastrar(ClienteRequisicao requisicao) {
        String cnpj = validarCnpj(requisicao.cnpj(), null);
        Cliente cliente = new Cliente();
        ClienteMapper.aplicar(requisicao, cliente, cnpj);
        Cliente salvo = clienteDao.inserir(cliente);
        log.info("Cliente cadastrado: #{} - {}", salvo.getId(), salvo.getRazaoSocial());
        return salvo;
    }

    /**
     * Atualiza os dados de um cliente existente.
     *
     * @throws RecursoNaoEncontradoException quando o id não existe
     */
    @Transactional
    public Cliente atualizar(Long id, ClienteRequisicao requisicao) {
        Cliente cliente = buscarPorId(id);
        String cnpj = validarCnpj(requisicao.cnpj(), id);
        ClienteMapper.aplicar(requisicao, cliente, cnpj);
        return clienteDao.atualizar(cliente);
    }

    /**
     * Remove um cliente. As reuniões vinculadas não são apagadas: apenas perdem
     * o vínculo, preservando o histórico de análises já feito.
     */
    @Transactional
    public void remover(Long id) {
        Cliente cliente = buscarPorId(id);
        int desvinculadas = reuniaoDao.desvincularCliente(id);
        clienteDao.remover(cliente);
        log.info("Cliente #{} removido ({} reuniao(oes) desvinculada(s))", id, desvinculadas);
    }

    /** Busca um cliente pelo id. */
    public Cliente buscarPorId(Long id) {
        return clienteDao.buscarPorId(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de(RECURSO, id));
    }

    /** Busca um cliente pelo CNPJ (com ou sem máscara). */
    public Cliente buscarPorCnpj(String cnpj) {
        String digitos = ValidadorCnpj.normalizar(cnpj);
        return clienteDao.buscarPorCnpj(digitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado para o CNPJ " + cnpj + "."));
    }

    /** Pesquisa paginada por razão social, nome fantasia ou CNPJ. */
    public List<Cliente> pesquisar(String termo, StatusCliente status, String uf, int pagina, int tamanho) {
        return clienteDao.pesquisar(termo, status, uf, pagina, tamanho);
    }

    /** Total de registros correspondentes aos mesmos critérios de {@link #pesquisar}. */
    public long contarPesquisa(String termo, StatusCliente status, String uf) {
        return clienteDao.contarPesquisa(termo, status, uf);
    }

    // ── Consultas de apoio ────────────────────────────────────────────

    /** Reuniões já vinculadas a um cliente. */
    public List<Reuniao> listarReunioes(Long clienteId) {
        garantirExistencia(clienteId);
        return reuniaoDao.listarPorCliente(clienteId);
    }

    /** Quantidade de reuniões vinculadas a um cliente. */
    public long contarReunioes(Long clienteId) {
        return reuniaoDao.contarPorCliente(clienteId);
    }

    /** Distribuição da carteira por situação — usada no dashboard. */
    public Map<StatusCliente, Long> contarPorStatus() {
        Map<StatusCliente, Long> totais = new EnumMap<>(StatusCliente.class);
        for (StatusCliente status : StatusCliente.values()) {
            totais.put(status, clienteDao.contarPorStatus(status));
        }
        return totais;
    }

    /** Total de clientes cadastrados. */
    public long contar() {
        return clienteDao.contar();
    }

    /** Garante que o cliente existe antes de operações que apenas o referenciam. */
    public void garantirExistencia(Long id) {
        if (!clienteDao.existePorId(id)) {
            throw RecursoNaoEncontradoException.de(RECURSO, id);
        }
    }

    // ── Validações internas ───────────────────────────────────────────

    /**
     * Normaliza e valida o CNPJ informado.
     *
     * @param idIgnorado id do próprio cliente em edição ({@code null} na inclusão)
     * @return CNPJ apenas com dígitos
     */
    private String validarCnpj(String cnpjInformado, Long idIgnorado) {
        if (!ValidadorCnpj.isValido(cnpjInformado)) {
            throw new RegraNegocioException("O CNPJ informado é inválido: " + cnpjInformado + ".");
        }
        String cnpj = ValidadorCnpj.normalizar(cnpjInformado);
        if (clienteDao.existeOutroComCnpj(cnpj, idIgnorado)) {
            throw new ConflitoDeDadosException("Já existe um cliente cadastrado com o CNPJ " + cnpjInformado + ".");
        }
        return cnpj;
    }
}
