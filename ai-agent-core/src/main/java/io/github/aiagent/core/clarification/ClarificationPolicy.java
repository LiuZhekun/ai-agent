package io.github.aiagent.core.clarification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 澄清子系统的外部化配置，通过 {@code ai.agent.clarification.*} 前缀绑定。
 * <p>
 * 核心配置项：
 * <ul>
 *   <li><b>maxRounds</b> —— 最大追问轮次（默认 3），防止陷入无限追问循环</li>
 *   <li><b>askBatchSize</b> —— 每轮最多追问的问题数（默认 2），控制单轮问题量以优化用户体验</li>
 *   <li><b>timeoutMinutes</b> —— 澄清超时时间（默认 10 分钟），超时后视为放弃</li>
 *   <li><b>onMaxRoundsExceeded</b> —— 达到最大轮次后的处理策略：中止或降级兜底</li>
 * </ul>
 *
 * @see ClarificationManager 使用本配置控制追问流程
 */
@Component
@ConfigurationProperties(prefix = "ai.agent.clarification")
public class ClarificationPolicy {

    private int maxRounds = 3;
    private int askBatchSize = 2;
    private int timeoutMinutes = 10;
    private OnMaxRoundsExceeded onMaxRoundsExceeded = OnMaxRoundsExceeded.ABORT;

    /**
     * 达到最大追问轮次后的处理策略。
     */
    public enum OnMaxRoundsExceeded {
        /** 直接中止任务，向用户返回无法完成的提示。 */
        ABORT,
        /** 使用已收集到的部分参数进行降级处理，尽量给出有意义的回复。 */
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
