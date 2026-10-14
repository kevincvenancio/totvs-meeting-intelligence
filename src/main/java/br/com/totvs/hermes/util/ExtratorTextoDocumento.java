package br.com.totvs.hermes.util;

import br.com.totvs.hermes.exception.ArquivoInvalidoException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Extrai o texto puro de documentos de transcrição (.txt e .docx).
 * <p>
 * O .docx é lido direto do pacote zip ({@code word/document.xml}) com o parser
 * StAX do JDK, sem dependência extra: cada parágrafo vira uma linha, e tabulações
 * e quebras de linha manuais são preservadas. Texto de revisões excluídas
 * ({@code w:delText}) e códigos de campo ficam de fora.
 */
public final class ExtratorTextoDocumento {

    private static final String DOCUMENTO_PRINCIPAL = "word/document.xml";

    /** Teto do XML descompactado — protege contra arquivos zip "bomba". */
    private static final long LIMITE_XML_BYTES = 200L * 1024 * 1024;

    private ExtratorTextoDocumento() {}

    /**
     * Lê o arquivo conforme a extensão.
     *
     * @throws ArquivoInvalidoException quando a extensão não é suportada ou o .docx está corrompido
     */
    public static String extrair(File arquivo) throws IOException {
        String nome = arquivo.getName().toLowerCase();
        if (nome.endsWith(".txt")) {
            return decodificar(Files.readAllBytes(arquivo.toPath()));
        }
        if (nome.endsWith(".docx")) {
            return extrairDocx(arquivo);
        }
        throw new ArquivoInvalidoException("Formato não suportado para transcrição: " + arquivo.getName());
    }

    /**
     * Decodifica bytes de texto tentando UTF-8 e caindo para as codificações
     * Latin-1/Windows usadas por exportações antigas. Remove o BOM inicial.
     */
    public static String decodificar(byte[] bytes) {
        String texto = null;
        for (String charset : new String[]{"UTF-8", "ISO-8859-1", "windows-1252"}) {
            try {
                String s = new String(bytes, Charset.forName(charset));
                if (!s.contains("�")) { texto = s; break; }
            } catch (java.nio.charset.UnsupportedCharsetException ignored) {}
        }
        if (texto == null) texto = new String(bytes, StandardCharsets.ISO_8859_1);
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }

    private static String extrairDocx(File arquivo) throws IOException {
        try (ZipFile zip = new ZipFile(arquivo)) {
            ZipEntry entrada = zip.getEntry(DOCUMENTO_PRINCIPAL);
            if (entrada == null) {
                throw new ArquivoInvalidoException("O arquivo .docx não contém o documento principal do Word.");
            }
            try (InputStream xml = new EntradaLimitada(zip.getInputStream(entrada), LIMITE_XML_BYTES)) {
                return lerParagrafos(xml);
            }
        } catch (ZipException excecao) {
            throw new ArquivoInvalidoException(
                    "O arquivo não é um .docx válido. Arquivos .doc antigos precisam ser salvos como .docx.", excecao);
        }
    }

    private static String lerParagrafos(InputStream xml) throws IOException {
        XMLInputFactory fabrica = XMLInputFactory.newFactory();
        fabrica.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        fabrica.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        StringBuilder texto = new StringBuilder();
        boolean dentroDeTexto = false;
        int profundidadeTabulacoes = 0; // <w:tabs> define paradas de tabulação, não é conteúdo
        try {
            XMLStreamReader leitor = fabrica.createXMLStreamReader(xml);
            while (leitor.hasNext()) {
                int evento = leitor.next();
                if (evento == XMLStreamConstants.START_ELEMENT) {
                    switch (leitor.getLocalName()) {
                        case "t"     -> dentroDeTexto = true;
                        case "tabs"  -> profundidadeTabulacoes++;
                        case "tab"   -> { if (profundidadeTabulacoes == 0) texto.append('\t'); }
                        case "br", "cr" -> texto.append('\n');
                        default      -> { }
                    }
                } else if (evento == XMLStreamConstants.END_ELEMENT) {
                    switch (leitor.getLocalName()) {
                        case "t"    -> dentroDeTexto = false;
                        case "tabs" -> profundidadeTabulacoes--;
                        case "p"    -> texto.append('\n');
                        default     -> { }
                    }
                } else if (dentroDeTexto && (evento == XMLStreamConstants.CHARACTERS
                        || evento == XMLStreamConstants.CDATA)) {
                    texto.append(leitor.getText());
                }
            }
            leitor.close();
        } catch (XMLStreamException excecao) {
            throw new ArquivoInvalidoException("O conteúdo do .docx está corrompido e não pôde ser lido.", excecao);
        }
        return texto.toString();
    }

    /** InputStream que falha ao ultrapassar o limite de bytes lidos. */
    private static final class EntradaLimitada extends FilterInputStream {
        private final long limite;
        private long lidos;

        EntradaLimitada(InputStream origem, long limite) {
            super(origem);
            this.limite = limite;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) contar(1);
            return b;
        }

        @Override
        public int read(byte[] destino, int inicio, int tamanho) throws IOException {
            int n = super.read(destino, inicio, tamanho);
            if (n > 0) contar(n);
            return n;
        }

        private void contar(int n) throws IOException {
            lidos += n;
            if (lidos > limite) {
                throw new IOException("documento descompactado excede o limite de " + (limite / 1024 / 1024) + " MB");
            }
        }
    }
}
