package io.github.aiagent.core.agent;

import io.github.aiagent.core.exception.SessionBusyException;
import io.github.aiagent.core.model.ChatMessage;
import io.github.aiagent.core.planner.TaskPlan;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Agent 会话上下文。
 */
public class AgentSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private final List<ExecutionTrace> executionTraces = new ArrayList<>();
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private final Instant createdAt = Instant.now();
    private Instant lastAccessedAt = Instant.now();
    private transient ReentrantLock lock = new ReentrantLock();
    private TaskPlan currentPlan;

    public AgentSession(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 尝试获取会话锁；失败时抛出可恢复异常，避免并发污染会话状态。
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    public void tryLock(long timeout, TimeUnit unit) {
        boolean locked;
        try {
            locked = lock.tryLock(timeout, unit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SessionBusyException(sessionId);
        }
        if (!locked) {
            throw new SessionBusyException(sessionId);
        }
    }

    /**
     * 释放会话锁。
     */
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<ChatMessage> getConversationHistory() {
        return conversationHistory;
    }

    public TaskPlan getCurrentPlan() {
        return currentPlan;
    }

    public void setCurrentPlan(TaskPlan currentPlan) {
        this.currentPlan = currentPlan;
    }

    public List<ExecutionTrace> getExecutionTraces() {
        return executionTraces;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void touch() {
        this.lastAccessedAt = Instant.now();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
