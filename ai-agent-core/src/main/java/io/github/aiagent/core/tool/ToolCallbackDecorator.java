package io.github.aiagent.core.tool;

import io.github.aiagent.core.exception.ToolExecutionException;
import io.github.aiagent.core.metrics.AgentMetrics;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 工具回调装饰器，提供限流、超时、重试与统一格式化能力。
 * <p>
 * 调用链：beforeCall 拦截 -> 受控执行 delegate -> afterCall 拦截 -> 统一格式化 -> 指标记录。
 */
public class ToolCallbackDecorator implements ToolCallback {

    private final ToolCallback delegate;
    private final List<ToolCallbackInterceptor> interceptors;
    private final AgentMetrics metrics;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final Semaphore concurrencyLimiter;
    private final ToolResultFormatter formatter;

    public ToolCallbackDecorator(
            ToolCallback delegate,
            List<ToolCallbackInterceptor> interceptors,
            AgentMetrics metrics,
            int timeoutSeconds,
            int maxRetries,
            Semaphore concurrencyLimiter,
            ToolResultFormatter formatter) {
        this.delegate = delegate;
        this.interceptors = interceptors;
        this.metrics = metrics;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.concurrencyLimiter = concurrencyLimiter;
        this.formatter = formatter;
    }

    @Override
    public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        // 并发闸门：避免单工具被并发洪峰击穿。
        boolean acquired = concurrencyLimiter.tryAcquire();
        if (!acquired) {
            throw new ToolExecutionException("Tool concurrency limit reached", getToolDefinition().name(), toolInput);
        }
        long start = System.currentTimeMillis();
        try {
            String effectiveInput = toolInput;
            for (ToolCallbackInterceptor interceptor : interceptors) {
                interceptor.beforeCall(getToolDefinition().name(), effectiveInput);
            }

            String output = null;
            Throwable lastError = null;
            for (int i = 0; i <= maxRetries; i++) {
                try {
                    String finalInput = effectiveInput;
                    // 使用异步 + 超时控制包装底层工具调用，避免长时间阻塞主流程。
                    output = CompletableFuture
                            .supplyAsync(() -> delegate.call(finalInput))
                            .get(timeoutSeconds, TimeUnit.SECONDS);
                    break;
                } catch (Exception ex) {
                    lastError = ex;
                }
            }
            if (output == null) {
                throw new ToolExecutionException("Tool execution failed after retries", getToolDefinition().name(), toolInput, lastError);
            }
            for (ToolCallbackInterceptor interceptor : interceptors) {
                output = interceptor.afterCall(getToolDefinition().name(), effectiveInput, output);
            }
            String formatted = formatter.format(output, ToolResultFormatter.FormatHint.AUTO);
            if (metrics != null) {
                metrics.recordToolCall(getToolDefinition().name(), true, System.currentTimeMillis() - start);
            }
            return formatted;
        } finally {
            concurrencyLimiter.release();
        }
    }

    @Override
    public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }
}
