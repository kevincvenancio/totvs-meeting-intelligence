package br.com.totvs.hermes.service;

import br.com.totvs.hermes.exception.ArquivoInvalidoException;
import br.com.totvs.hermes.util.ExtratorTextoDocumento;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Testes manuais da importação de transcrições avulsas (.txt e .docx).
 * Padrão: apenas try-catch, sem JUnit/AssertJ/Mockito.
 */
public class TranscricaoArquivoServiceTest {

    private static int passou = 0, falhou = 0;

    interface Teste { void executar() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("hermes-teste-transcricao-");

        // Teste 1 — cabeçalho entre "---" vira campos e sai da transcrição
        executar("Teste 1 — cabeçalho preenche campos", () -> {
            String doc = "---\nCliente: Construtora Alfa\nData: 2025-03-10\nDuração: 45min\nVendedor: T123\n---\n"
                    + "Vendedor: Bom dia!\nCliente: Bom dia.";
            Map<String, String> c = TranscricaoArquivoService.montarCampos(doc, "qualquer.txt");
            esperar("Construtora Alfa", c.get("NOME_UNIDADE"), "cliente");
            esperar("2025-03-10", c.get("DT_MEETING"), "data");
            esperar("45min", c.get("DURACAO_MEETING"), "duração");
            esperar("T123", c.get("CODT"), "vendedor");
            esperar("Vendedor: Bom dia!\nCliente: Bom dia.", c.get("ANON_TRANSCRICAO"), "transcrição");
        });

        // Teste 2 — sem cabeçalho, linhas "Cliente: ..." continuam sendo fala, não metadado
        executar("Teste 2 — sem cabeçalho usa o nome do arquivo", () -> {
            String doc = "Cliente: estamos avaliando trocar de sistema.\n\nVendedor: entendo.";
            Map<String, String> c = TranscricaoArquivoService.montarCampos(doc, "Acme Ltda_2025-04-02.docx");
            esperar("Acme Ltda", c.get("NOME_UNIDADE"), "cliente");
            esperar("2025-04-02", c.get("DT_MEETING"), "data");
            esperar(doc, c.get("ANON_TRANSCRICAO"), "transcrição");
            if (!c.get("ID_MEETING").startsWith("ARQ-")) throw new AssertionError("id gerado: " + c.get("ID_MEETING"));
        });

        // Teste 3 — mesmo texto gera o mesmo id (reimportação cai como duplicada)
        executar("Teste 3 — id estável pelo conteúdo", () -> {
            String a = TranscricaoArquivoService.montarCampos("Olá,  mundo\r\n", "a.txt").get("ID_MEETING");
            String b = TranscricaoArquivoService.montarCampos("olá, mundo", "b.txt").get("ID_MEETING");
            esperar(a, b, "id");
        });

        // Teste 4 — data no formato brasileiro no nome do arquivo
        executar("Teste 4 — data dd-MM-yyyy no nome", () -> {
            Map<String, String> c = TranscricaoArquivoService.montarCampos("texto", "reuniao 10-03-2025.txt");
            esperar("10/03/2025", c.get("DT_MEETING"), "data");
            esperar("reuniao", c.get("NOME_UNIDADE"), "cliente");
        });

        // Teste 5 — .txt em UTF-8 com BOM e em Windows-1252
        executar("Teste 5 — .txt com BOM e Windows-1252", () -> {
            File utf8 = pasta.resolve("bom.txt").toFile();
            Files.write(utf8.toPath(), ("\uFEFFReunião de implantação").getBytes(StandardCharsets.UTF_8));
            esperar("Reunião de implantação", ExtratorTextoDocumento.extrair(utf8), "utf-8");

            File ansi = pasta.resolve("ansi.txt").toFile();
            Files.write(ansi.toPath(), "Reunião de implantação".getBytes("windows-1252"));
            esperar("Reunião de implantação", ExtratorTextoDocumento.extrair(ansi), "windows-1252");
        });

        // Teste 6 — .docx: parágrafos, tabulação e quebra; ignora paradas de tab e texto excluído
        executar("Teste 6 — .docx extrai parágrafos", () -> {
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"
                    + "<w:p><w:pPr><w:tabs><w:tab w:val=\"left\" w:pos=\"720\"/></w:tabs></w:pPr>"
                    + "<w:r><w:t>Vendedor:</w:t></w:r><w:r><w:tab/><w:t xml:space=\"preserve\">Bom dia </w:t></w:r>"
                    + "<w:r><w:delText>apagado</w:delText></w:r><w:r><w:t>&amp; boa semana</w:t></w:r></w:p>"
                    + "<w:p><w:r><w:t>Cliente: linha 1</w:t><w:br/><w:t>linha 2</w:t></w:r></w:p>"
                    + "</w:body></w:document>";
            File docx = criarDocx(pasta, "teste.docx", xml);
            esperar("Vendedor:\tBom dia & boa semana\nCliente: linha 1\nlinha 2\n",
                    ExtratorTextoDocumento.extrair(docx), "texto");
        });

        // Teste 7 — arquivo .docx que não é zip é recusado com mensagem clara
        executar("Teste 7 — .docx inválido", () -> {
            File falso = pasta.resolve("falso.docx").toFile();
            Files.writeString(falso.toPath(), "isto não é um docx");
            try {
                ExtratorTextoDocumento.extrair(falso);
                throw new AssertionError("esperava ArquivoInvalidoException");
            } catch (ArquivoInvalidoException esperado) {
                // ok
            }
        });

        // Teste 8 — DTD/entidade externa no XML não é processada (XXE)
        executar("Teste 8 — .docx com DTD é recusado", () -> {
            String xml = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///c:/windows/win.ini\">]>"
                    + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                    + "<w:body><w:p><w:r><w:t>&e;</w:t></w:r></w:p></w:body></w:document>";
            File docx = criarDocx(pasta, "xxe.docx", xml);
            try {
                String texto = ExtratorTextoDocumento.extrair(docx);
                if (texto.contains("[")) throw new AssertionError("entidade externa foi resolvida");
            } catch (ArquivoInvalidoException esperado) {
                // ok — o parser recusou a entidade
            }
        });

        System.out.println("\nResultado: " + passou + " passou(ram) | " + falhou + " falhou(ram)");
        if (falhou > 0) System.exit(1);
    }

    private static void executar(String nome, Teste teste) {
        try {
            teste.executar();
            passou++;
            System.out.println("[OK] " + nome);
        } catch (AssertionError e) {
            falhou++;
            System.out.println("[FALHA] " + nome + " — " + e.getMessage());
        } catch (Exception e) {
            falhou++;
            System.out.println("[ERRO] " + nome + " — exceção inesperada: " + e);
        }
    }

    private static void esperar(String esperado, String obtido, String campo) {
        if (!esperado.equals(obtido)) {
            throw new AssertionError(campo + ": esperava [" + esperado + "] mas veio [" + obtido + "]");
        }
    }

    private static File criarDocx(Path pasta, String nome, String documentXml) throws Exception {
        File arquivo = pasta.resolve(nome).toFile();
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(arquivo))) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return arquivo;
    }
}
