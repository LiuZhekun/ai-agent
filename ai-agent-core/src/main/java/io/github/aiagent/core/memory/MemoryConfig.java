package io.github.aiagent.core.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 记忆子系统的外部化配置，通过 {@code ai.agent.memory.*} 前缀绑定。
 * <p>
 * 核心配置项：
 * <ul>
 *   <li><b>strategy</b> —— 对话历史裁剪策略，决定 {@link AgentMemoryManager#loadAndTrim(String)}
 *       的行为，详见 {@link Strategy} 枚举</li>
 *   <li><b>messageWindow</b> —— MESSAGE_WINDOW 策略下保留的最大消息条数（默认 20）</li>
 *   <li><b>maxTokens</b> —— TOKEN_WINDOW 策略下的 Token 预算上限（默认 8000）</li>
 *   <li><b>summaryThreshold</b> —— SUMMARY 策略下触发摘要压缩的 Token 阈值（默认 6000）</li>
 *   <li><b>vectorTopK / vectorThreshold</b> —— 向量知识库检索参数</li>
 * </ul>
 *
 * @see AgentMemoryManager 使用本配置驱动记忆裁剪和向量检索
 */
@Component
@ConfigurationProperties(prefix = "ai.agent.memory")
public class MemoryConfig {

    private Strategy strategy = Strategy.MESSAGE_WINDOW;
    private int messageWindow = 20;
    private int maxTokens = 8000;
    private int summaryThreshold = 6000;
    private int vectorTopK = 5;
    private double vectorThreshold = 0.7D;

    /**
     * 对话历史裁剪策略枚举。
     * <p>
     * 三种策略在效果与成本之间提供了不同的权衡：
     * <ul>
     *   <li>{@link #MESSAGE_WINDOW} —— 最简单，零额外开销，但可能丢失重要上下文</li>
     *   <li>{@link #TOKEN_WINDOW} —— 精确控制 Token，适合对成本敏感的场景</li>
     *   <li>{@link #SUMMARY} —— 保留最多语义信息，但需要额外 LLM 调用</li>
     * </ul>
     */
    public enum Strategy {
        /** 固定消息条数滑窗，超出时丢弃最早的消息。 */
        MESSAGE_WINDOW,
        /** 基于 Token 计数的滑窗，使用 jtokkit 精确计算后裁剪。 */
        TOKEN_WINDOW,
        /** 超过阈值时调用 LLM 将前半段历史压缩为摘要。 */
        SUMMARY
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public int getMessageWindow() {
        return messageWindow;
    }

    public void setMessageWindow(int messageWindow) {
        this.messageWindow = messageWindow;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getSummaryThreshold() {
        return summaryThreshold;
    }

    public void setSummaryThreshold(int summaryThreshold) {
        this.summaryThreshold = summaryThreshold;
    }

    public int getVectorTopK() {
        return vectorTopK;
    }

    public void setVectorTopK(int vectorTopK) {
        this.vectorTopK = vectorTopK;
    }

    public double getVectorThreshold() {
        return vectorThreshold;
    }

    public void setVectorThreshold(double vectorThreshold) {
        this.vectorThreshold = vectorThreshold;
    }
}
