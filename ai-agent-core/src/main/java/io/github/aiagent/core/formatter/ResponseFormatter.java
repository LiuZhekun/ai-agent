package io.github.aiagent.core.formatter;

import io.github.aiagent.core.model.AgentEvent;
import io.github.aiagent.core.model.AgentEventType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 响应格式化统一入口。
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
     * 根据类型格式化并生成事件。
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
