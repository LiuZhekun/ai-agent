package io.github.aiagent.core.formatter;

import io.github.aiagent.core.model.AgentEvent;
import io.github.aiagent.core.model.AgentEventType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 响应格式化统一入口，根据 {@link FormatType} 将数据转换为对应格式并封装为 {@link AgentEvent}。
 * <p>
 * 本类是格式化子系统的门面，内部委托给 {@link TableFormatter} 和 {@link ChartFormatter}
 * 处理具体的格式化逻辑。调用方只需指定数据和目标格式，无需关心格式化细节。
 * <p>
 * 格式化规则：
 * <ul>
 *   <li>{@link FormatType#MARKDOWN_TABLE} → 生成 Markdown 表格，事件类型 FINAL_ANSWER</li>
 *   <li>{@link FormatType#ECHART_BAR} / {@link FormatType#ECHART_LINE} / {@link FormatType#ECHART_PIE}
 *       → 生成 ECharts option JSON，事件类型 CHART_PAYLOAD</li>
 *   <li>其他类型 → 直接 toString() 输出，事件类型 FINAL_ANSWER</li>
 * </ul>
 *
 * @see FormatType 支持的格式类型
 * @see TableFormatter 表格格式化器
 * @see ChartFormatter 图表格式化器
 */
@Component
public class ResponseFormatter {

    private final TableFormatter tableFormatter;
    private final ChartFormatter chartFormatter;

    public ResponseFormatter(TableFormatter tableFormatter, ChartFormatter chartFormatter) {
        this.tableFormatter = tableFormatter;
        this.chartFormatter = chartFormatter;
    }

    /**
     * 将数据按指定格式转换，并封装为 {@link AgentEvent}。
     *
     * @param data      待格式化的原始数据，表格/图表类型要求为 {@code List<Map<String, Object>>}
     * @param type      目标输出格式
     * @param sessionId 会话 ID，用于构建事件
     * @return 包含格式化结果的 AgentEvent
     */
    public AgentEvent format(Object data, FormatType type, String sessionId) {
        if (type == FormatType.MARKDOWN_TABLE && data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            String table = tableFormatter.toMarkdownTable((List<Map<String, Object>>) data);
            return AgentEvent.of(AgentEventType.FINAL_ANSWER, sessionId, table);
        }
        if ((type == FormatType.ECHART_BAR || type == FormatType.ECHART_LINE || type == FormatType.ECHART_PIE)
                && data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            String chart = chartFormatter.toEChartsOption((List<Map<String, Object>>) data, type);
            return AgentEvent.of(AgentEventType.CHART_PAYLOAD, sessionId, chart);
        }
        return AgentEvent.of(AgentEventType.FINAL_ANSWER, sessionId, String.valueOf(data));
    }
}
