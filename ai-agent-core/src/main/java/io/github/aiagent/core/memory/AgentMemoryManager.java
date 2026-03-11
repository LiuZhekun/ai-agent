package io.github.aiagent.core.memory;

import io.github.aiagent.core.model.ChatMessage;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一记忆管理入口，整合短期对话记忆与长期向量知识检索，是 Agent 记忆子系统的核心门面。
 *
 * <h3>双层记忆架构</h3>
 * <p>
 * 本类采用"短期 + 长期"双层记忆设计，分别解决不同场景下的上下文需求：
 * <ul>
 *   <li><b>短期记忆（对话历史）</b> —— 基于 {@link ConversationMemory} 存储近期对话消息，
 *       通过 {@link #loadAndTrim(String)} 按 {@link MemoryConfig.Strategy} 指定的策略裁剪上下文，
 *       防止 Token 超限。三种策略分别对应不同的成本/效果权衡：
 *       <ul>
 *         <li>{@code MESSAGE_WINDOW} —— 简单丢弃最早的消息，零额外开销</li>
 *         <li>{@code TOKEN_WINDOW} —— 使用 jtokkit 精确计算 Token 后裁剪，精度高但需要额外计算</li>
 *         <li>{@code SUMMARY} —— 调用 LLM 生成摘要压缩历史，保留更多语义但产生额外 API 调用</li>
 *       </ul>
 *   </li>
 *   <li><b>长期记忆（向量知识库）</b> —— 通过 {@link VectorKnowledgeBase} 接口进行语义检索，
 *       当 Spring 容器中存在实现 Bean 时自动启用；未接入时 {@link #searchKnowledge(String)}
 *       返回空列表，不影响正常对话流程。这一设计允许在不改动核心代码的情况下可选接入向量能力。</li>
 * </ul>
 *
 * @see MemoryConfig 记忆系统配置（策略选择、窗口大小、向量检索参数等）
 * @see ConversationMemory 对话历史存储抽象接口
 * @see VectorKnowledgeBase 向量知识库检索抽象接口
 */
@Component
public class AgentMemoryManager {

    private final ConversationMemory conversationMemory;
    private final TokenBudgetTrimmer tokenBudgetTrimmer;
    private final SummaryCompressor summaryCompressor;
    @Nullable
    private final VectorKnowledgeBase vectorKnowledgeBase;
    private final MemoryConfig memoryConfig;

    public AgentMemoryManager(
            ConversationMemory conversationMemory,
            TokenBudgetTrimmer tokenBudgetTrimmer,
            SummaryCompressor summaryCompressor,
            @Nullable VectorKnowledgeBase vectorKnowledgeBase,
            MemoryConfig memoryConfig) {
        this.conversationMemory = conversationMemory;
        this.tokenBudgetTrimmer = tokenBudgetTrimmer;
        this.summaryCompressor = summaryCompressor;
        this.vectorKnowledgeBase = vectorKnowledgeBase;
        this.memoryConfig = memoryConfig;
    }

    /**
     * 加载指定会话的对话历史，并按 {@link MemoryConfig#getStrategy()} 配置的策略进行裁剪。
     * <p>
     * 裁剪策略由配置驱动，调用方无需感知具体实现，只需获取裁剪后的消息列表注入 Prompt 即可。
     *
     * @param sessionId 会话唯一标识
     * @return 裁剪后的对话历史列表；会话无历史时返回空列表
     */
    public List<ChatMessage> loadAndTrim(String sessionId) {
        List<ChatMessage> history = conversationMemory.load(sessionId);
        if (history.isEmpty()) {
            return history;
        }
        return switch (memoryConfig.getStrategy()) {
            case MESSAGE_WINDOW -> messageWindow(history, memoryConfig.getMessageWindow());
            case TOKEN_WINDOW -> tokenBudgetTrimmer.trim(history, memoryConfig.getMaxTokens());
            case SUMMARY -> summaryCompressor.compress(history, memoryConfig.getSummaryThreshold());
        };
    }

    /**
     * 将一条消息追加到指定会话的对话历史中。
     *
     * @param sessionId 会话唯一标识
     * @param message   待保存的消息
     */
    public void save(String sessionId, ChatMessage message) {
        conversationMemory.save(sessionId, message);
    }

    /**
     * 长期记忆检索 —— 在向量知识库中按语义相似度召回相关文本片段。
     * <p>
     * 检索参数由 {@link MemoryConfig} 控制：
     * <ul>
     *   <li>{@code vector-top-k} — 最大召回条数（默认 5）</li>
     *   <li>{@code vector-threshold} — 相似度阈值（默认 0.7），低于此值的结果被过滤</li>
     * </ul>
     * <p>
     * 当 {@link VectorKnowledgeBase} 未注入时（即未引入 vectorizer 模块或未实现该接口），
     * 返回空列表，不影响对话主链路。
     *
     * @param query 用户查询文本
     * @return 语义匹配的文本片段列表，按相似度降序；未接入向量能力时返回空列表
     */
    public List<String> searchKnowledge(String query) {
        if (vectorKnowledgeBase == null) {
            return List.of();
        }
        return vectorKnowledgeBase.search(query, memoryConfig.getVectorTopK(), memoryConfig.getVectorThreshold());
    }

    private List<ChatMessage> messageWindow(List<ChatMessage> history, int window) {
        if (history.size() <= window) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - window, history.size()));
    }
}
