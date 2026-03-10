package io.github.aiagent.knowledge.sql;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL 频率限制器。
 */
@Component
public class SqlRateLimiter {

    private final SqlQueryProperties properties;
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private volatile Instant windowStart = Instant.now();

    public SqlRateLimiter(SqlQueryProperties properties) {
        this.properties = properties;
    }

    public synchronized boolean allow(String sessionId) {
        if (Instant.now().isAfter(windowStart.plusSeconds(60))) {
            counters.clear();
            windowStart = Instant.now();
        }
        AtomicInteger count = counters.computeIfAbsent(sessionId, key -> new AtomicInteger(0));
        return count.incrementAndGet() <= properties.getRateLimit();
    }
}
