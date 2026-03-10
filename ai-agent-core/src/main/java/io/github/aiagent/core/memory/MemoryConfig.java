package io.github.aiagent.core.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 记忆系统配置。
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

    public enum Strategy {
        /** 固定消息窗口。 */
        MESSAGE_WINDOW,
        /** 固定 Token 窗口。 */
        TOKEN_WINDOW,
        /** 摘要压缩策略。 */
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
