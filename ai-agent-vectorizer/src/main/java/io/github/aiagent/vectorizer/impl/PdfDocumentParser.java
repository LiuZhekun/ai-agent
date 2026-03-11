package io.github.aiagent.vectorizer.impl;

import io.github.aiagent.vectorizer.DocumentParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 文档解析器，基于 Apache PDFBox。
 * <p>
 * 按页提取文本内容，每页作为一个独立的文本片段返回，
 * 便于后续按页粒度进行向量化和检索定位。
 * 空白页会被自动跳过。
 */
@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public List<String> parse(InputStream input, String filename) {
        try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            int totalPages = document.getNumberOfPages();
            List<String> pages = new ArrayList<>(totalPages);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document).trim();
                if (!pageText.isEmpty()) {
                    pages.add(pageText);
                }
            }
            return pages;
        } catch (Exception ex) {
            throw new IllegalStateException("Parse pdf failed: " + filename, ex);
        }
    }
}
