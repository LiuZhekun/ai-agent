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
 * 会话管理器，负责会话生命周期与 TTL 清理。
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
     * 按需获取或创建会话。
     *
     * @param sessionId 会话 ID
     * @return 会话对象
     */
    public AgentSession getOrCreateSession(String sessionId) {
        AgentSession session = sessions.computeIfAbsent(sessionId, AgentSession::new);
        session.touch();
        return session;
    }

    public AgentSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

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

    @PreDestroy
    public void shutdown() {
        cleaner.shutdown();
    }
}
