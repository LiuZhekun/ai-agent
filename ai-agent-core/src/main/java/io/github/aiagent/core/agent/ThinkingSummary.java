package io.github.aiagent.core.agent;

import java.io.Serial;
import java.io.Serializable;

/**
 * 面向用户的思考过程摘要 —— 从 {@link ThinkingProcess} 提炼而来。
 *
 * <p>通过 {@link SummaryLevel} 控制摘要的详细程度：</p>
 * <ul>
 *   <li>{@code NONE} —— 不展示，适用于简单查询；</li>
 *   <li>{@code SUMMARY} —— 展示关键推理步骤的简要概述（默认级别）；</li>
 *   <li>{@code DETAILED} —— 展示完整的推理逻辑链路，适用于调试或需要透明度的场景。</li>
 * </ul>
 *
 * <p>该对象会随 {@link io.github.aiagent.core.model.AgentEvent}
 * ({@code THINKING_SUMMARY} 类型) 通过 SSE 推送至前端。</p>
 *
 * @see ThinkingProcess
 */
public class ThinkingSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String summary;
    private SummaryLevel level = SummaryLevel.SUMMARY;

    /**
     * 摘要展示级别。
     */
    public enum SummaryLevel {
        /** 不展示摘要。 */
        NONE,
        /** 展示简要摘要。 */
        SUMMARY,
        /** 展示详细摘要。 */
        DETAILED
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public SummaryLevel getLevel() {
        return level;
    }

    public void setLevel(SummaryLevel level) {
        this.level = level;
    }
}
