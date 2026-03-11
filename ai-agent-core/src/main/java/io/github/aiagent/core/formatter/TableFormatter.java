package io.github.aiagent.core.formatter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表格格式化器，将 {@code List<Map>} 形式的行列数据转换为 Markdown 或 HTML 表格。
 * <p>
 * 表头取自第一行数据的 key 集合（依赖 {@link java.util.LinkedHashMap} 保持顺序），
 * 因此上游数据源应确保使用有序 Map 以获得稳定的列顺序。
 *
 * @see ResponseFormatter 格式化入口（本类的主要调用方）
 */
@Component
public class TableFormatter {

    /**
     * 将数据转换为 Markdown 格式的表格字符串。
     *
     * @param data 行列数据，每个 Map 代表一行，key 为列名
     * @return Markdown 表格字符串；数据为空时返回 "暂无数据"
     */
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
     * 将数据转换为 HTML {@code <table>} 格式的字符串。
     * <p>
     * 当前默认响应优先使用 Markdown，本方法多用于嵌入 Web 页面或邮件模板等场景。
     *
     * @param data 行列数据，每个 Map 代表一行，key 为列名
     * @return HTML 表格字符串；数据为空时返回 {@code <p>暂无数据</p>}
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
