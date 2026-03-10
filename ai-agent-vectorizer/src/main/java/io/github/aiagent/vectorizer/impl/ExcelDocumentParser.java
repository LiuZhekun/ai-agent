package io.github.aiagent.vectorizer.impl;

import io.github.aiagent.vectorizer.DocumentParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 文档解析器。
 */
@Component
public class ExcelDocumentParser implements DocumentParser {
    @Override
    public List<String> parse(InputStream input, String filename) {
        try (var wb = WorkbookFactory.create(input)) {
            List<String> lines = new ArrayList<>();
            for (Sheet sheet : wb) {
                for (Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (Cell cell : row) {
                        line.append(cell.getAddress()).append(":").append(cell.toString()).append(" ");
                    }
                    lines.add(line.toString().trim());
                }
            }
            return lines;
        } catch (Exception ex) {
            throw new IllegalStateException("Parse excel failed: " + filename, ex);
        }
    }
}
