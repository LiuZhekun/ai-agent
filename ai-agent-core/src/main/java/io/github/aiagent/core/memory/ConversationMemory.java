package io.github.aiagent.core.memory;

import io.github.aiagent.core.model.ChatMessage;

import java.util.List;

/**
 * 对话记忆存储接口。
 */
public interface ConversationMemory {

    void save(String sessionId, ChatMessage message);

    List<ChatMessage> load(String sessionId);

    void clear(String sessionId);

    int estimateTokens(String sessionId);
}
