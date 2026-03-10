package io.github.aiagent.vectorizer.impl;

import io.github.aiagent.vectorizer.DocumentParser;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Word 文档解析器。
 */
@Component
public class WordDocumentParser implements DocumentParser {
    @Override
    public List<String> parse(InputStream input, String filename) {
        try (XWPFDocument doc = new XWPFDocument(input)) {
            String text = doc.getParagraphs().stream().map(XWPFParagraph::getText).collect(Collectors.joining("\n"));
            return List.of(text);
        } catch (Exception ex) {
            throw new IllegalStateException("Parse word failed: " + filename, ex);
        }
    }
}
