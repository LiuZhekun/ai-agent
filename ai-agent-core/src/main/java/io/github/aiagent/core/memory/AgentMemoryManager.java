package io.github.aiagent.core.memory;

import io.github.aiagent.core.model.ChatMessage;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一记忆管理入口。
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
     * 加载并按策略裁剪历史。
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

    public void save(String sessionId, ChatMessage message) {
        conversationMemory.save(sessionId, message);
    }

    /**
     * 长期记忆检索；未接入向量能力时返回空列表。
     * 该方法是长期记忆统一入口，当前主链路未默认启用，保留给知识增强场景按需接入。
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
