package io.github.aiagent.core.formatter;

/**
 * 输出格式类型。
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
