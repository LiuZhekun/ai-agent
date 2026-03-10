package io.github.aiagent.core.agent;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * LLM 原始思考过程（内部对象，不直接对外返回）。
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
