package io.github.aiagent.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Agent 指标采集器。
 * <p>
 * 指标方法多数由装饰器/拦截器在运行时调用，不一定在核心业务代码里出现直接引用。
 */
@Component
public class AgentMetrics {

    private final MeterRegistry meterRegistry;

    public AgentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录模型 token 消耗。
     */
    public void recordTokenUsage(int promptTokens, int completionTokens) {
        Counter.builder("agent.token.consumed").register(meterRegistry).increment(promptTokens + completionTokens);
    }

    /**
     * 记录工具调用次数与耗时。
     */
    public void recordToolCall(String toolName, boolean success, long durationMs) {
        Counter.builder("agent.tool.calls").tag("tool.name", toolName).tag("success", String.valueOf(success)).register(meterRegistry).increment();
        Timer.builder("agent.tool.duration").tag("tool.name", toolName).register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 SQL 被安全策略拒绝的次数。
     */
    public void recordSqlRejection(String reason) {
        Counter.builder("agent.sql.rejected").tag("reason", reason).register(meterRegistry).increment();
    }

    /**
     * 记录会话总耗时。
     */
    public void recordSessionDuration(String sessionId, long durationMs) {
        Timer.builder("agent.session.duration").tag("session.id", sessionId).register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 更新活跃会话数指标。
     */
    public void setActiveSessions(int count) {
        meterRegistry.gauge("agent.session.active", count);
    }
}
