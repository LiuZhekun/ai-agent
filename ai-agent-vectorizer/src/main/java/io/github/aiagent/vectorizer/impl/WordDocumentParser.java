package io.github.aiagent.vectorizer.impl;

import io.github.aiagent.vectorizer.DocumentParser;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Word (.docx) 文档解析器，基于 Apache POI。
 * <p>
 * 提取文档中所有段落的文本内容，用换行符拼接后作为整体返回。
 * 仅支持 .docx 格式（Office Open XML），不支持旧版 .doc 格式。
 */
@Component
public class WordDocumentParser implements DocumentParser {

    @Override
    public List<String> parse(InputStream input, String filename) {
        try (XWPFDocument doc = new XWPFDocument(input)) {
            String text = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
            return List.of(text);
        } catch (Exception ex) {
            throw new IllegalStateException("Parse word failed: " + filename, ex);
        }
    }
}
