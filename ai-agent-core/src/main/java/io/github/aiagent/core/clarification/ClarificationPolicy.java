package io.github.aiagent.core.clarification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 澄清策略配置。
 */
@Component
@ConfigurationProperties(prefix = "ai.agent.clarification")
public class ClarificationPolicy {

    private int maxRounds = 3;
    private int askBatchSize = 2;
    private int timeoutMinutes = 10;
    private OnMaxRoundsExceeded onMaxRoundsExceeded = OnMaxRoundsExceeded.ABORT;

    public enum OnMaxRoundsExceeded {
        /** 达到上限后中止。 */
        ABORT,
        /** 达到上限后降级兜底。 */
        FALLBACK
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public int getAskBatchSize() {
        return askBatchSize;
    }

    public void setAskBatchSize(int askBatchSize) {
        this.askBatchSize = askBatchSize;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public OnMaxRoundsExceeded getOnMaxRoundsExceeded() {
        return onMaxRoundsExceeded;
    }

    public void setOnMaxRoundsExceeded(OnMaxRoundsExceeded onMaxRoundsExceeded) {
        this.onMaxRoundsExceeded = onMaxRoundsExceeded;
    }
}
