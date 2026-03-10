package io.github.aiagent.core.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工具结果格式化器，统一转换为 LLM 友好文本。
 */
@Component
public class ToolResultFormatter {

    private final ObjectMapper objectMapper;

    public ToolResultFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public enum FormatHint {
        /** 自动推断格式。 */
        AUTO,
        /** 强制纯文本。 */
        PLAIN_TEXT,
        /** 强制 Markdown 表格。 */
        MARKDOWN_TABLE,
        /** 强制 JSON。 */
        JSON
    }

    public String format(Object result, FormatHint hint) {
        if (result == null) {
            return "null";
        }
        if (hint == FormatHint.PLAIN_TEXT) {
            return String.valueOf(result);
        }
        if (hint == FormatHint.JSON) {
            return asJson(result);
        }
        if (result instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            return formatAsMarkdownTable((List<Map<String, Object>>) result);
        }
        if (result instanceof Map<?, ?> || result instanceof List<?>) {
            return asJson(result);
        }
        return String.valueOf(result);
    }

    private String asJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String formatAsMarkdownTable(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "暂无数据";
        }
        List<String> headers = rows.get(0).keySet().stream().toList();
        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        headers.forEach(h -> sb.append(h).append(" | "));
        sb.append('\n').append("| ");
        headers.forEach(h -> sb.append("--- | "));
        sb.append('\n');
        for (Map<String, Object> row : rows) {
            sb.append("| ");
            for (String header : headers) {
                Object value = row.get(header);
                sb.append(value == null ? "" : value).append(" | ");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
