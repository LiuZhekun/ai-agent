package io.github.aiagent.core.tool;

import io.github.aiagent.core.exception.ToolExecutionException;
import io.github.aiagent.core.metrics.AgentMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 工具回调装饰器 —— 以装饰器模式为原始 {@link ToolCallback} 叠加企业级治理能力。
 *
 * <h3>调用链路（每次 {@link #call} 触发）</h3>
 * <pre>
 *   并发闸门（Semaphore.tryAcquire）
 *     → 拦截器链 beforeCall()
 *       → 带超时的异步执行 delegate.call()（含重试）
 *     → 拦截器链 afterCall()
 *     → ToolResultFormatter 统一格式化
 *     → AgentMetrics 指标记录
 *   → 释放闸门（Semaphore.release，在 finally 中）
 * </pre>
 *
 * <h3>各治理能力说明</h3>
 * <ul>
 *   <li><b>限流</b> —— 共享 {@link java.util.concurrent.Semaphore} 控制全局工具并发数，
 *       避免单工具洪峰导致下游服务过载；</li>
 *   <li><b>超时</b> —— 通过 {@link java.util.concurrent.CompletableFuture#get(long, java.util.concurrent.TimeUnit)}
 *       限制单次调用最大耗时；</li>
 *   <li><b>重试</b> —— 可配置最大重试次数 {@code maxRetries}，对瞬态错误进行透明重试；</li>
 *   <li><b>拦截</b> —— 调用前后分别执行 {@link ToolCallbackInterceptor} 链，
 *       支持日志、审计、参数改写等 SPI 扩展；</li>
 *   <li><b>格式化</b> —— 通过 {@link ToolResultFormatter} 将结果转为 LLM 友好文本。</li>
 * </ul>
 *
 * @see AgentToolCallbackProvider
 * @see ToolCallbackInterceptor
 * @see ToolResultFormatter
 */
public class ToolCallbackDecorator implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ToolCallbackDecorator.class);

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

    /**
     * 执行带有完整治理链路的工具调用。
     *
     * <p>整个调用过程包括：并发限流 → 拦截器前置处理 → 异步超时执行（含重试）
     * → 拦截器后置处理 → 结果格式化 → 指标采集。任何环节失败都会抛出
     * {@link io.github.aiagent.core.exception.ToolExecutionException}。</p>
     *
     * @param toolInput JSON 格式的工具输入参数（由 LLM 生成）
     * @return 格式化后的工具执行结果文本
     * @throws io.github.aiagent.core.exception.ToolExecutionException 并发超限或重试耗尽时
     */
    @Override
    public String call(String toolInput) {
        String toolName = getToolDefinition().name();
        log.info("工具调用开始: tool={}, input={}", toolName, truncate(toolInput));
        // 并发闸门：避免单工具被并发洪峰击穿。
        boolean acquired = concurrencyLimiter.tryAcquire();
        if (!acquired) {
            throw new ToolExecutionException("Tool concurrency limit reached", getToolDefinition().name(), toolInput);
        }
        long start = System.currentTimeMillis();
        try {
            String effectiveInput = toolInput;
            for (ToolCallbackInterceptor interceptor : interceptors) {
                effectiveInput = interceptor.beforeCall(getToolDefinition().name(), effectiveInput);
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
                    lastError = unwrapExecutionException(ex);
                    if (isNonRetryable(lastError)) {
                        break;
                    }
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
            log.info("工具调用完成: tool={}, durationMs={}, output={}",
                    toolName, System.currentTimeMillis() - start, truncate(formatted));
            return formatted;
        } catch (RuntimeException ex) {
            Throwable rootError = ex.getCause() != null ? ex.getCause() : ex;
            log.warn("工具调用失败: tool={}, durationMs={}, input={}, error={}",
                    toolName, System.currentTimeMillis() - start, truncate(toolInput), describeError(rootError));
            throw ex;
        } finally {
            concurrencyLimiter.release();
        }
    }

    @Override
    public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300) + "...";
    }

    private Throwable unwrapExecutionException(Throwable throwable) {
        if (throwable instanceof ExecutionException executionException && executionException.getCause() != null) {
            return executionException.getCause();
        }
        return throwable;
    }

    private boolean isNonRetryable(Throwable throwable) {
        return throwable instanceof IllegalArgumentException || throwable instanceof IllegalStateException;
    }

    private String describeError(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }
}
