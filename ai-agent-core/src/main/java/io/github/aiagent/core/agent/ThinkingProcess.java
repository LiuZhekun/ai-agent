package io.github.aiagent.core.agent;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * LLM 原始思考过程 —— 捕获模型推理的中间产物。
 *
 * <p>此对象属于内部数据结构，<b>不直接</b>对前端返回。
 * 其作用是保留 LLM 的完整推理文本（{@link #rawContent}）和 token 消耗量，
 * 供 {@link io.github.aiagent.core.agent.advisor.ThinkingSummaryAdvisor}
 * 进一步提炼为面向用户的 {@link ThinkingSummary}。</p>
 *
 * <p>设计上与 {@link ThinkingSummary} 分离，是为了在安全和隐私层面控制
 * 暴露给终端用户的信息粒度。</p>
 *
 * @see ThinkingSummary
 */
public class ThinkingProcess implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String rawContent;
    private Instant timestamp;
    private int tokenCount;

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
}
