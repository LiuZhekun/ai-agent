package io.github.aiagent.core.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图表格式化器。
 */
@Component
public class ChartFormatter {

    private final ObjectMapper objectMapper;

    public ChartFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toEChartsOption(List<Map<String, Object>> data, FormatType chartType) {
        if (data == null || data.isEmpty()) {
            return "{\"title\":{\"text\":\"暂无数据\"},\"series\":[]}";
        }
        String xField = data.get(0).keySet().stream().findFirst().orElse("x");
        String yField = data.get(0).keySet().stream().skip(1).findFirst().orElse(xField);
        Map<String, Object> option = new HashMap<>();
        option.put("title", Map.of("text", "数据图表"));
        option.put("tooltip", Map.of());
        option.put("legend", Map.of("data", List.of(yField)));
        option.put("xAxis", Map.of("type", "category", "data", data.stream().map(r -> r.get(xField)).toList()));
        option.put("yAxis", Map.of("type", "value"));
        String seriesType = switch (chartType) {
            case ECHART_PIE -> "pie";
            case ECHART_LINE -> "line";
            default -> "bar";
        };
        option.put("series", List.of(Map.of(
                "name", yField,
                "type", seriesType,
                "data", data.stream().map(r -> r.get(yField)).toList()
        )));
        try {
            return objectMapper.writeValueAsString(option);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Build echart option failed", ex);
        }
    }
}
