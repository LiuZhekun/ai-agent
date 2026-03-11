package io.github.aiagent.core.memory;

import io.github.aiagent.core.model.ChatMessage;

import java.util.List;

/**
 * 对话记忆存储接口，定义短期对话历史的持久化契约。
 * <p>
 * 所有对话记忆的存储实现（如 Redis、内存、数据库等）都应实现此接口。
 * {@link AgentMemoryManager} 通过此接口与具体存储解耦，调用方只需关注
 * "保存/加载/清除/估算 Token" 四个核心操作。
 * <p>
 * 设计约定：
 * <ul>
 *   <li>所有方法以 {@code sessionId} 作为隔离维度，不同会话的数据互不干扰</li>
 *   <li>{@link #load(String)} 应返回按时间升序排列的完整对话历史</li>
 *   <li>{@link #estimateTokens(String)} 用于在裁剪前快速评估上下文大小，
 *       允许使用近似算法以减少计算开销</li>
 * </ul>
 *
 * @see ConversationMemoryRedisImpl 基于 Redis 的默认实现
 * @see AgentMemoryManager 记忆管理器（本接口的主要消费方）
 */
public interface ConversationMemory {

    /**
     * 将一条消息追加到指定会话的对话历史末尾。
     *
     * @param sessionId 会话唯一标识
     * @param message   待保存的对话消息
     */
    void save(String sessionId, ChatMessage message);

    /**
     * 加载指定会话的全部对话历史。
     *
     * @param sessionId 会话唯一标识
     * @return 按时间升序排列的消息列表；会话不存在或无历史时返回空列表
     */
    List<ChatMessage> load(String sessionId);

    /**
     * 清除指定会话的所有对话历史。
     *
     * @param sessionId 会话唯一标识
     */
    void clear(String sessionId);

    /**
     * 估算指定会话当前对话历史的 Token 总数。
     * <p>
     * 此方法用于在执行裁剪策略前快速判断是否需要裁剪，
     * 实现可以使用近似算法（如按字符长度折算）以降低计算开销。
     *
     * @param sessionId 会话唯一标识
     * @return 估算的 Token 数量
     */
    int estimateTokens(String sessionId);
}
