package io.github.aiagent.core.agent;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理器 —— 管理所有 {@link AgentSession} 的生命周期。
 *
 * <h3>存储策略</h3>
 * <p>使用 {@link java.util.concurrent.ConcurrentHashMap} 作为内存存储，
 * 天然支持多线程并发的 get/put/remove 操作，无需额外加锁。
 * 适用于单实例或会话亲和（sticky session）部署模式。</p>
 *
 * <h3>过期清理</h3>
 * <p>通过 {@link java.util.concurrent.ScheduledExecutorService} 每分钟执行一次扫描，
 * 移除 {@code lastAccessedAt} 超过可配置 TTL（默认 30 分钟，见 {@code ai.agent.session.ttl-minutes}）
 * 的会话。这是一种"惰性 + 周期性"混合清理策略：
 * <ul>
 *   <li>惰性：每次访问时调用 {@link AgentSession#touch()} 续期；</li>
 *   <li>周期性：定时任务兜底清除已无人访问的会话。</li>
 * </ul>
 *
 * <h3>扩展方向</h3>
 * <p>若需支持多实例部署，可将存储替换为 Redis 等分布式方案，
 * 同时保持 {@link #getOrCreateSession} 的接口契约不变。</p>
 *
 * @see AgentSession
 */
@Component
public class AgentSessionManager {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final Duration sessionTtl;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public AgentSessionManager(@Value("${ai.agent.session.ttl-minutes:30}") long ttlMinutes) {
        this.sessionTtl = Duration.ofMinutes(ttlMinutes);
        cleaner.scheduleAtFixedRate(this::cleanExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 按需获取已有会话或创建新会话，同时刷新其最后访问时间。
     *
     * <p>内部使用 {@code computeIfAbsent} 保证对同一 sessionId 的并发调用只会创建一个实例。</p>
     *
     * @param sessionId 会话唯一标识（通常由前端生成的 UUID）
     * @return 已存在或新创建的会话对象
     */
    public AgentSession getOrCreateSession(String sessionId) {
        AgentSession session = sessions.computeIfAbsent(sessionId, AgentSession::new);
        session.touch();
        return session;
    }

    /**
     * 根据 sessionId 查找会话，不存在时返回 {@code null}。
     *
     * @param sessionId 会话唯一标识
     * @return 会话对象；若不存在则为 {@code null}
     */
    public AgentSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 主动移除指定会话。通常用于用户显式结束会话或管理端口强制清理。
     *
     * @param sessionId 要移除的会话标识
     */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * 返回当前活跃会话数量，供监控和健康检查使用。
     *
     * @return 活跃会话数
     */
    public int activeSessionCount() {
        return sessions.size();
    }

    private void cleanExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> {
            AgentSession session = entry.getValue();
            return Duration.between(session.getLastAccessedAt(), now).compareTo(sessionTtl) > 0;
        });
    }

    /**
     * 容器销毁时关闭定时清理线程池，防止资源泄漏。
     */
    @PreDestroy
    public void shutdown() {
        cleaner.shutdown();
    }
}
