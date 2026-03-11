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
 * Excel (.xlsx / .xls) 文档解析器，基于 Apache POI。
 * <p>
 * 遍历所有 Sheet 的每一行，将单元格按 {@code 列地址:值} 格式拼接为一条文本。
 * 每行作为一个独立的文本片段返回，适合对表格数据逐行向量化。
 * <p>
 * 输出示例：{@code A1:张三 B1:13800138000 C1:zhangsan@example.com}
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
                    String text = line.toString().trim();
                    if (!text.isEmpty()) {
                        lines.add(text);
                    }
                }
            }
            return lines;
        } catch (Exception ex) {
            throw new IllegalStateException("Parse excel failed: " + filename, ex);
        }
    }
}
