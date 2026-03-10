package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.AgentRequest;
import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.memory.AgentMemoryManager;
import io.github.aiagent.core.model.ChatMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 记忆增强 Advisor。
 * 在调用前加载会话历史，在调用后写入本轮消息。
 */
@Component
@Order(10)
public class MemoryAdvisor {

    private final AgentMemoryManager memoryManager;

    public MemoryAdvisor(AgentMemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    /**
     * 调用前读取并回填历史。
     */
    public List<ChatMessage> before(AgentSession session) {
        return memoryManager.loadAndTrim(session.getSessionId());
    }

    /**
     * 调用后保存本轮用户消息。
     */
    public void after(AgentRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return;
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRole(ChatMessage.Role.USER);
        chatMessage.setContent(request.getMessage());
        chatMessage.setTimestamp(Instant.now());
        memoryManager.save(request.getSessionId(), chatMessage);
    }

    /**
     * 保存助手回复到对话记忆，保证多轮上下文完整。
     */
    public void saveAssistant(String sessionId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRole(ChatMessage.Role.ASSISTANT);
        chatMessage.setContent(content);
        chatMessage.setTimestamp(Instant.now());
        memoryManager.save(sessionId, chatMessage);
    }
}
