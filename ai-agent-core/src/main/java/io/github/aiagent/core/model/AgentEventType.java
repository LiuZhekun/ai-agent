package io.github.aiagent.core.model;

import java.io.Serializable;

/**
 * Agent SSE 事件类型枚举。
 */
public enum AgentEventType implements Serializable {
    /** 心跳事件。 */
    HEARTBEAT,
    /** 思考摘要事件。 */
    THINKING_SUMMARY,
    /** 需要用户澄清。 */
    CLARIFICATION_REQUIRED,
    /** 槽位更新事件。 */
    SLOT_UPDATE,
    /** 执行确认事件。 */
    EXECUTION_CONFIRM_REQUIRED,
    /** 工具执行轨迹。 */
    TOOL_TRACE,
    /** 图表数据载荷。 */
    CHART_PAYLOAD,
    /** 最终答案事件。 */
    FINAL_ANSWER,
    /** 错误事件。 */
    ERROR,
    /** 完成事件。 */
    COMPLETED
}
