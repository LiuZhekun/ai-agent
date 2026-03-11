package io.github.aiagent.core.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工具结果格式化器 —— 将工具输出统一转换为 LLM 可理解的文本。
 *
 * <h3>设计意图</h3>
 * <p>不同工具的返回类型各异（Map、List、POJO、String 等），
 * 直接传给 LLM 可能导致解析混乱或 token 浪费。本格式化器根据
 * {@link FormatHint} 和运行时类型自动选择最佳表示形式：</p>
 * <ul>
 *   <li>{@code List<Map>} → Markdown 表格（适合结构化查询结果）；</li>
 *   <li>{@code Map / List} → JSON 文本；</li>
 *   <li>其他类型 → {@code String.valueOf} 纯文本。</li>
 * </ul>
 *
 * <p>由 {@link ToolCallbackDecorator} 在拦截器链末尾统一调用。</p>
 *
 * @see ToolCallbackDecorator
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

    /**
     * 根据格式提示和运行时类型，将工具输出转换为 LLM 友好的文本。
     *
     * @param result 工具执行的原始返回值
     * @param hint   格式提示；{@code AUTO} 时自动推断
     * @return 格式化后的文本
     */
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
