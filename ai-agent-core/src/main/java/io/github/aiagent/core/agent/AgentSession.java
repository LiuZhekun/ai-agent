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
 * Agent 会话上下文 —— 承载单个对话生命周期内的所有状态。
 *
 * <h3>职责</h3>
 * <p>每个 {@code AgentSession} 对应一次用户会话（由 sessionId 标识），包含：</p>
 * <ul>
 *   <li>对话历史 ({@link #conversationHistory}) —— 完整的消息序列；</li>
 *   <li>执行轨迹 ({@link #executionTraces}) —— 本次会话中所有工具调用的审计记录；</li>
 *   <li>任务计划 ({@link #currentPlan}) —— 由 {@link io.github.aiagent.core.agent.advisor.PlanningAdvisor}
 *       生成的可选任务规划；</li>
 *   <li>元数据 ({@link #metadata}) —— Advisor 之间传递的临时上下文键值对。</li>
 * </ul>
 *
 * <h3>并发模型</h3>
 * <p>使用 {@link java.util.concurrent.locks.ReentrantLock} 保证同一会话在同一时刻只能被
 * 一个请求处理。{@link AgentEngine#chat} 在处理请求前会调用 {@link #tryLock}，
 * 处理完毕后在 finally 中调用 {@link #unlock}。该设计避免了多个并发请求同时修改
 * 会话历史而产生数据竞争。</p>
 *
 * <h3>生命周期</h3>
 * <p>会话由 {@link AgentSessionManager} 创建和管理，超过 TTL 未访问时会被自动清理。
 * 每次被访问时应调用 {@link #touch()} 刷新最后访问时间。</p>
 *
 * <p>注意：{@code lock} 字段标记为 {@code transient}，序列化/反序列化后需要重新初始化。</p>
 *
 * @see AgentEngine
 * @see AgentSessionManager
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
     * 尝试在指定时间内获取会话独占锁。
     *
     * <p>若获取失败（超时或线程被中断），将抛出 {@link SessionBusyException}，
     * 调用方可据此向用户返回"会话繁忙"的可恢复提示。</p>
     *
     * @param timeout 等待超时时间
     * @param unit    超时时间单位
     * @throws SessionBusyException 若在超时时间内未能获取锁
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
     * 释放会话独占锁。仅当当前线程持有锁时才执行释放，避免 {@link IllegalMonitorStateException}。
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

    /**
     * 刷新最后访问时间为当前时刻，用于 {@link AgentSessionManager} 的 TTL 过期判断。
     */
    public void touch() {
        this.lastAccessedAt = Instant.now();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
