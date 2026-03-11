package io.github.aiagent.knowledge.sql;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL 会话级频率限制器 —— {@link SafeSqlQueryTool} 多层防御的第三道关卡。
 * <p>
 * 采用固定窗口（60 秒）+ 按 sessionId 计数的策略，防止 LLM 在短时间内
 * 高频调用 SQL 工具对数据库造成压力。窗口到期后自动重置所有计数器。
 * <p>
 * 每分钟允许的最大调用次数通过 {@link SqlQueryProperties#getRateLimit()} 配置。
 *
 * @see SafeSqlQueryTool
 * @see SqlQueryProperties
 */
@Component
public class SqlRateLimiter {

    private final SqlQueryProperties properties;
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private volatile Instant windowStart = Instant.now();

    public SqlRateLimiter(SqlQueryProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断指定会话是否允许执行本次 SQL 查询。
     * <p>
     * 每个 60 秒窗口内，同一 sessionId 的调用次数不得超过 {@code rateLimit}。
     *
     * @param sessionId 会话标识
     * @return {@code true} 允许执行，{@code false} 超出频率限制
     */
    public synchronized boolean allow(String sessionId) {
        if (Instant.now().isAfter(windowStart.plusSeconds(60))) {
            counters.clear();
            windowStart = Instant.now();
        }
        AtomicInteger count = counters.computeIfAbsent(sessionId, key -> new AtomicInteger(0));
        return count.incrementAndGet() <= properties.getRateLimit();
    }
}
