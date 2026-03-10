package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.ThinkingSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 思考摘要生成 Advisor。
 */
@Component
@Order(50)
public class ThinkingSummaryAdvisor {

    private final ThinkingSummary.SummaryLevel level;

    public ThinkingSummaryAdvisor(@Value("${ai.agent.thinking.level:SUMMARY}") String level) {
        this.level = ThinkingSummary.SummaryLevel.valueOf(level.toUpperCase());
    }

    /**
     * 基于原始响应构造对外摘要，避免泄露细节推理。
     */
    public ThinkingSummary summarize(String rawOutput) {
        ThinkingSummary summary = new ThinkingSummary();
        summary.setLevel(level);
        if (level == ThinkingSummary.SummaryLevel.NONE) {
            summary.setSummary("");
            return summary;
        }
        if (rawOutput == null || rawOutput.isBlank()) {
            summary.setSummary("本轮无可提取的思考摘要。");
            return summary;
        }
        String content = rawOutput.length() > 120 ? rawOutput.substring(0, 120) + "..." : rawOutput;
        summary.setSummary(level == ThinkingSummary.SummaryLevel.DETAILED ? "详细摘要：" + content : "摘要：" + content);
        return summary;
    }
}
