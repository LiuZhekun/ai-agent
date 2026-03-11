package io.github.aiagent.core.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ECharts 图表格式化器，将行列数据转换为 ECharts option JSON 字符串。
 * <p>
 * 前端接收到 {@code CHART_PAYLOAD} 类型的事件后，可直接将 payload 传入
 * {@code echarts.setOption()} 进行渲染。
 * <p>
 * 字段映射约定：取第一行数据的第一个 key 作为 X 轴字段，第二个 key 作为 Y 轴字段。
 * 因此上游数据源应确保 Map 中前两个 key 分别对应维度和指标。
 *
 * @see ResponseFormatter 格式化入口（本类的主要调用方）
 * @see FormatType#ECHART_BAR
 * @see FormatType#ECHART_LINE
 * @see FormatType#ECHART_PIE
 */
@Component
public class ChartFormatter {

    private final ObjectMapper objectMapper;

    public ChartFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将行列数据转换为 ECharts option JSON 字符串。
     *
     * @param data      行列数据，每个 Map 代表一个数据点
     * @param chartType 图表类型（BAR / LINE / PIE）
     * @return ECharts option JSON 字符串；数据为空时返回包含"暂无数据"标题的空图表 JSON
     */
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
