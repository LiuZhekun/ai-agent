package io.github.aiagent.core.agent;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对外可展示的思考摘要。
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
