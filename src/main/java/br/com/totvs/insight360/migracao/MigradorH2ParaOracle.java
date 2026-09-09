package br.com.totvs.insight360.migracao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copia os dados já analisados do banco H2 local para o Oracle da FIAP.
 * <p>
 * Executado apenas sob o perfil {@code migracao}:
 *
 * <pre>mvn spring-boot:run -Dspring-boot.run.profiles=migracao</pre>
 *
 * A cópia é genérica: as colunas são descobertas nos catálogos dos dois bancos e
 * apenas as existentes em ambos são transferidas, preservando os identificadores
 * originais para não quebrar as chaves estrangeiras. Ao final, a sequência da
 * coluna de identidade é reposicionada para continuar após o maior id copiado.
 * <p>
 * A operação é segura para repetir: tabelas que já possuem dados no destino são
 * ignoradas, evitando duplicação.
 */
@Component
@Profile("migracao")
@RequiredArgsConstructor
@Slf4j
public class MigradorH2ParaOracle implements ApplicationRunner {

    /** Ordem de cópia respeitando as chaves estrangeiras. */
    private static final List<String> TABELAS = List.of(
            "importacao_lotes", "clientes", "usuarios", "reunioes",
            "insights", "feedbacks_reuniao", "planos_acao", "comentarios_reuniao");

    private static final String COLUNA_CHAVE = "id";
    private static final int TAMANHO_PAGINA = 100;
    private static final int REGISTROS_POR_COMMIT = 200;

    /** Acima deste tamanho o texto e enviado como CLOB temporario, nao por stream. */
    private static final int LIMITE_TEXTO_DIRETO = 8_000;

    private final DataSource dataSourceDestino;

    @Value("${insight360.migracao.origem.url}")
    private String urlOrigem;

    @Value("${insight360.migracao.origem.usuario}")
    private String usuarioOrigem;

    @Value("${insight360.migracao.origem.senha}")
    private String senhaOrigem;

    @Override
    public void run(ApplicationArguments argumentos) throws SQLException {
        log.info("Iniciando migracao H2 -> Oracle | origem: {}", urlOrigem);
        long inicio = System.currentTimeMillis();
        Map<String, Long> resumo = new LinkedHashMap<>();

        try (Connection origem = DriverManager.getConnection(urlOrigem, usuarioOrigem, senhaOrigem);
             Connection destino = dataSourceDestino.getConnection()) {

            destino.setAutoCommit(false);
            for (String tabela : TABELAS) {
                resumo.put(tabela, migrarTabela(origem, destino, tabela));
            }
        }

        long segundos = (System.currentTimeMillis() - inicio) / 1000;
        log.info("Migracao concluida em {}s", segundos);
        resumo.forEach((tabela, total) -> log.info("  {}: {} registro(s)", tabela, total));
    }

    // ── Cópia de uma tabela ───────────────────────────────────────────

    /**
     * Copia uma tabela inteira.
     *
     * @return quantidade de registros copiados (0 quando a tabela foi ignorada)
     */
    private long migrarTabela(Connection origem, Connection destino, String tabela) throws SQLException {
        long totalOrigem = contar(origem, tabela);
        if (totalOrigem == 0) {
            log.info("{}: sem dados na origem — ignorada", tabela);
            return 0;
        }
        long totalDestino = contar(destino, tabela);
        if (totalDestino > 0) {
            log.warn("{}: destino ja possui {} registro(s) — ignorada para nao duplicar",
                    tabela, totalDestino);
            return 0;
        }

        Map<String, ColunaDestino> colunasDestino = lerColunasDestino(destino, tabela);
        List<String> colunas = colunasComuns(origem, tabela, colunasDestino);
        log.info("{}: copiando {} registro(s) em {} coluna(s)", tabela, totalOrigem, colunas.size());

        permitirIdExplicito(destino, tabela);
        long copiados = copiarRegistros(origem, destino, tabela, colunas, colunasDestino, totalOrigem);
        reiniciarIdentidade(destino, tabela);
        return copiados;
    }

    private long copiarRegistros(Connection origem, Connection destino, String tabela,
                                 List<String> colunas, Map<String, ColunaDestino> colunasDestino,
                                 long totalOrigem) throws SQLException {

        String listaColunas = String.join(", ", colunas);
        String parametros = String.join(", ", colunas.stream().map(coluna -> "?").toList());
        String insercao = "INSERT INTO " + tabela + " (" + listaColunas + ") VALUES (" + parametros + ")";
        String leitura = "SELECT " + listaColunas + " FROM " + tabela
                + " WHERE " + COLUNA_CHAVE + " > ? ORDER BY " + COLUNA_CHAVE
                + " FETCH FIRST " + TAMANHO_PAGINA + " ROWS ONLY";

        long copiados = 0;
        long ultimoId = 0;
        boolean temMais = true;

        try (PreparedStatement consulta = origem.prepareStatement(leitura)) {
            while (temMais) {
                consulta.setLong(1, ultimoId);
                temMais = false;
                try (ResultSet pagina = consulta.executeQuery()) {
                    while (pagina.next()) {
                        temMais = true;
                        ultimoId = pagina.getLong(COLUNA_CHAVE);
                        gravarRegistro(destino, pagina, tabela, insercao, colunas, colunasDestino, ultimoId);
                        copiados++;

                        if (copiados % REGISTROS_POR_COMMIT == 0) {
                            destino.commit();
                            log.info("  {}: {}/{} registro(s)", tabela, copiados, totalOrigem);
                        }
                    }
                }
            }
            destino.commit();
        } catch (SQLException excecao) {
            destino.rollback();
            throw excecao;
        }
        log.info("{}: {} registro(s) copiados", tabela, copiados);
        return copiados;
    }

    /**
     * Grava um registro usando um {@link PreparedStatement} novo a cada linha.
     * <p>
     * O comando <b>nao</b> e reaproveitado de proposito: o driver Oracle fixa o
     * formato do bind na primeira execucao e, ao encontrar mais adiante uma
     * transcricao acima de 32 KB, tenta trocar para o protocolo LONG e falha com
     * ORA-01461. Um comando por registro deixa cada linha decidir o proprio bind,
     * e as transcricoes maiores ainda viajam como CLOB de verdade (locator),
     * evitando por completo o protocolo LONG.
     */
    private void gravarRegistro(Connection destino, ResultSet linha, String tabela, String insercao,
                                List<String> colunas, Map<String, ColunaDestino> colunasDestino,
                                long id) throws SQLException {
        List<Clob> temporarios = new ArrayList<>();
        try (PreparedStatement gravacao = destino.prepareStatement(insercao)) {
            for (int i = 0; i < colunas.size(); i++) {
                ligar(destino, gravacao, i + 1, linha, i + 1,
                        colunasDestino.get(colunas.get(i)), tabela, temporarios);
            }
            gravacao.executeUpdate();
        } catch (SQLException excecao) {
            log.error("{}: falha ao gravar o registro id={} | {}",
                    tabela, id, descreverRegistro(linha, colunas, colunasDestino));
            throw excecao;
        } finally {
            liberar(temporarios);
        }
    }

    /** Libera os CLOBs temporarios criados para o registro. */
    private void liberar(List<Clob> temporarios) {
        for (Clob clob : temporarios) {
            try {
                clob.free();
            } catch (SQLException excecao) {
                log.warn("Falha ao liberar CLOB temporario: {}", excecao.getMessage());
            }
        }
    }

    /**
     * Descreve as colunas textuais mais volumosas do registro que falhou, para
     * apontar rapidamente a origem do problema no log.
     */
    private String descreverRegistro(ResultSet linha, List<String> colunas,
                                     Map<String, ColunaDestino> colunasDestino) {
        List<String> detalhes = new ArrayList<>();
        for (int i = 0; i < colunas.size(); i++) {
            ColunaDestino coluna = colunasDestino.get(colunas.get(i));
            try {
                String valor = linha.getString(i + 1);
                if (valor != null && valor.length() > 1000) {
                    detalhes.add(coluna.nome() + "(tipo=" + coluna.tipo() + ", " + valor.length() + " chars)");
                }
            } catch (SQLException ignorado) {
                detalhes.add(coluna.nome() + "(ilegivel)");
            }
        }
        return detalhes.isEmpty() ? "sem colunas textuais grandes" : String.join(", ", detalhes);
    }

    // ── Ligação de valores ────────────────────────────────────────────

    /** Converte e vincula um valor da origem ao parâmetro correspondente no destino. */
    private void ligar(Connection destino, PreparedStatement gravacao, int indiceDestino, ResultSet linha,
                       int indiceOrigem, ColunaDestino coluna, String tabela,
                       List<Clob> temporarios) throws SQLException {
        switch (coluna.tipo()) {
            case Types.CLOB, Types.NCLOB, Types.LONGVARCHAR, Types.LONGNVARCHAR ->
                    ligarTextoLongo(destino, gravacao, indiceDestino,
                            linha.getString(indiceOrigem), temporarios);
            case Types.CHAR, Types.VARCHAR, Types.NCHAR, Types.NVARCHAR ->
                    ligarTexto(gravacao, indiceDestino, linha.getString(indiceOrigem), coluna, tabela);
            case Types.DATE -> {
                Date data = linha.getDate(indiceOrigem);
                if (data == null) {
                    gravacao.setNull(indiceDestino, Types.DATE);
                } else {
                    gravacao.setDate(indiceDestino, data);
                }
            }
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> {
                Timestamp instante = linha.getTimestamp(indiceOrigem);
                if (instante == null) {
                    gravacao.setNull(indiceDestino, Types.TIMESTAMP);
                } else {
                    gravacao.setTimestamp(indiceDestino, instante);
                }
            }
            default -> ligarValorSimples(gravacao, indiceDestino, linha.getObject(indiceOrigem), coluna);
        }
    }

    /**
     * CLOB: textos acima do limite do protocolo direto do Oracle (~32 KB, contados
     * em bytes) viajam como CLOB temporario (locator); os demais vao por stream.
     */
    private void ligarTextoLongo(Connection destino, PreparedStatement gravacao, int indice,
                                 String valor, List<Clob> temporarios) throws SQLException {
        if (valor == null) {
            gravacao.setNull(indice, Types.CLOB);
        } else if (valor.length() <= LIMITE_TEXTO_DIRETO) {
            gravacao.setCharacterStream(indice, new StringReader(valor), valor.length());
        } else {
            Clob clob = destino.createClob();
            clob.setString(1, valor);
            temporarios.add(clob);
            gravacao.setClob(indice, clob);
        }
    }

    /** VARCHAR2: trunca com aviso caso a origem tenha texto maior que a coluna de destino. */
    private void ligarTexto(PreparedStatement gravacao, int indice, String valor,
                            ColunaDestino coluna, String tabela) throws SQLException {
        if (valor == null) {
            gravacao.setNull(indice, Types.VARCHAR);
            return;
        }
        String texto = valor;
        if (coluna.tamanho() > 0 && texto.length() > coluna.tamanho()) {
            log.warn("{}.{}: valor com {} caracteres truncado para {}",
                    tabela, coluna.nome(), texto.length(), coluna.tamanho());
            texto = texto.substring(0, coluna.tamanho());
        }
        gravacao.setString(indice, texto);
    }

    /** Numeros e demais tipos; o boolean do H2 vira 0/1 no NUMBER(1) do Oracle. */
    private void ligarValorSimples(PreparedStatement gravacao, int indice, Object valor,
                                   ColunaDestino coluna) throws SQLException {
        if (valor == null) {
            gravacao.setNull(indice, coluna.tipo());
        } else if (valor instanceof Boolean logico) {
            gravacao.setInt(indice, Boolean.TRUE.equals(logico) ? 1 : 0);
        } else {
            gravacao.setObject(indice, valor);
        }
    }

    // ── Metadados ─────────────────────────────────────────────────────

    /** Lê nome, tipo e tamanho das colunas da tabela no Oracle. */
    private Map<String, ColunaDestino> lerColunasDestino(Connection destino, String tabela)
            throws SQLException {
        Map<String, ColunaDestino> colunas = new LinkedHashMap<>();
        try (ResultSet metadados = destino.getMetaData()
                .getColumns(null, destino.getSchema(), tabela.toUpperCase(), null)) {
            while (metadados.next()) {
                String nome = metadados.getString("COLUMN_NAME").toUpperCase();
                colunas.put(nome, new ColunaDestino(nome,
                        metadados.getInt("DATA_TYPE"), metadados.getInt("COLUMN_SIZE")));
            }
        }
        if (colunas.isEmpty()) {
            throw new IllegalStateException("Tabela " + tabela + " nao encontrada no Oracle.");
        }
        return colunas;
    }

    /** Interseção entre as colunas da origem e do destino, na ordem da origem. */
    private List<String> colunasComuns(Connection origem, String tabela,
                                       Map<String, ColunaDestino> colunasDestino) throws SQLException {
        List<String> comuns = new ArrayList<>();
        List<String> apenasNaOrigem = new ArrayList<>();
        try (Statement comando = origem.createStatement();
             ResultSet vazio = comando.executeQuery("SELECT * FROM " + tabela + " WHERE 1 = 0")) {
            ResultSetMetaData metadados = vazio.getMetaData();
            for (int i = 1; i <= metadados.getColumnCount(); i++) {
                String nome = metadados.getColumnName(i).toUpperCase();
                if (colunasDestino.containsKey(nome)) {
                    comuns.add(nome);
                } else {
                    apenasNaOrigem.add(nome);
                }
            }
        }
        if (!apenasNaOrigem.isEmpty()) {
            log.warn("{}: coluna(s) presente(s) apenas na origem e nao copiada(s): {}",
                    tabela, String.join(", ", apenasNaOrigem));
        }
        return comuns;
    }

    // ── Colunas de identidade ─────────────────────────────────────────

    /** Permite gravar o id vindo da origem (a coluna nasce como GENERATED ALWAYS). */
    private void permitirIdExplicito(Connection destino, String tabela) {
        executarDdl(destino, "ALTER TABLE " + tabela + " MODIFY (" + COLUNA_CHAVE
                + " GENERATED BY DEFAULT AS IDENTITY)");
    }

    /** Reposiciona a sequência da identidade para continuar após o maior id copiado. */
    private void reiniciarIdentidade(Connection destino, String tabela) {
        executarDdl(destino, "ALTER TABLE " + tabela + " MODIFY (" + COLUNA_CHAVE
                + " GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE))");
    }

    private void executarDdl(Connection destino, String ddl) {
        try (Statement comando = destino.createStatement()) {
            comando.execute(ddl);
        } catch (SQLException excecao) {
            log.warn("Falha ao executar [{}]: {}", ddl, excecao.getMessage());
        }
    }

    private long contar(Connection conexao, String tabela) throws SQLException {
        try (Statement comando = conexao.createStatement();
             ResultSet resultado = comando.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return resultado.next() ? resultado.getLong(1) : 0;
        }
    }
}
