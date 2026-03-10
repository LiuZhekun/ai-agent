package io.github.aiagent.vectorizer.impl;

import io.github.aiagent.vectorizer.DocumentParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * PDF 文档解析器。
 */
@Component
public class PdfDocumentParser implements DocumentParser {
    @Override
    public List<String> parse(InputStream input, String filename) {
        try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            String text = new PDFTextStripper().getText(document);
            return List.of(text);
        } catch (Exception ex) {
            throw new IllegalStateException("Parse pdf failed: " + filename, ex);
        }
    }
}
