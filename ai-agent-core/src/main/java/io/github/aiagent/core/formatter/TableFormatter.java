package io.github.aiagent.core.formatter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表格格式化器。
 */
@Component
public class TableFormatter {

    public String toMarkdownTable(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "暂无数据";
        }
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        headers.forEach(h -> sb.append(h).append(" | "));
        sb.append('\n').append("| ");
        headers.forEach(h -> sb.append("--- | "));
        sb.append('\n');
        for (Map<String, Object> row : data) {
            sb.append("| ");
            for (String h : headers) {
                Object val = row.get(h);
                sb.append(val == null ? "" : val).append(" | ");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * HTML 表格输出能力。
     * 当前默认响应优先使用 Markdown，因此该方法多用于嵌入 Web 页面或邮件模板等场景。
     */
    public String toHtmlTable(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "<p>暂无数据</p>";
        }
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        StringBuilder sb = new StringBuilder();
        sb.append("<table><thead><tr>");
        headers.forEach(h -> sb.append("<th>").append(h).append("</th>"));
        sb.append("</tr></thead><tbody>");
        for (Map<String, Object> row : data) {
            sb.append("<tr>");
            for (String h : headers) {
                Object v = row.get(h);
                sb.append("<td>").append(v == null ? "" : v).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }
}
