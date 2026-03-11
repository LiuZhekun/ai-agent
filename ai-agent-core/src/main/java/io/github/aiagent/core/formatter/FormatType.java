package io.github.aiagent.core.formatter;

/**
 * Agent 响应的输出格式类型枚举。
 * <p>
 * 由 {@link ResponseFormatter#format(Object, FormatType, String)} 根据此类型
 * 选择对应的格式化器进行数据转换。前端可根据事件类型（FINAL_ANSWER / CHART_PAYLOAD）
 * 和格式类型决定如何渲染响应内容。
 *
 * @see ResponseFormatter 使用本枚举驱动格式化分发
 */
public enum FormatType {
    /** 纯文本。 */
    TEXT,
    /** Markdown 表格。 */
    MARKDOWN_TABLE,
    /** HTML 表格。 */
    HTML_TABLE,
    /** ECharts 柱状图。 */
    ECHART_BAR,
    /** ECharts 折线图。 */
    ECHART_LINE,
    /** ECharts 饼图。 */
    ECHART_PIE,
    /** JSON 文本。 */
    JSON
}
