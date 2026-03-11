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
 * 记忆增强 Advisor —— 在 Advisor 链中负责对话历史的读写。
 *
 * <h3>在 Advisor 链中的位置</h3>
 * <p>{@code @Order(10)}，是最先执行的 Advisor。保证后续 Advisor 和 LLM
 * 都能看到完整的多轮对话上下文。</p>
 *
 * <h3>工作流程</h3>
 * <ul>
 *   <li><b>before</b> —— 从 {@link io.github.aiagent.core.memory.AgentMemoryManager}
 *       加载并修剪历史消息，将其注入 Session 元数据供 System Prompt 引用；</li>
 *   <li><b>after</b> —— 将本轮用户消息持久化到记忆存储；</li>
 *   <li><b>saveAssistant</b> —— 将 LLM 回复也持久化，确保下一轮对话时上下文完整。</li>
 * </ul>
 *
 * <p>注意：该 Advisor 由 {@link io.github.aiagent.core.agent.AgentEngine}
 * 在通用 Advisor 链之外显式调用，因为其 before/after 签名与
 * {@link io.github.aiagent.core.agent.AgentAdvisor} 接口不同。</p>
 *
 * @see io.github.aiagent.core.memory.AgentMemoryManager
 */
@Component
@Order(10)
public class MemoryAdvisor {

    private final AgentMemoryManager memoryManager;

    public MemoryAdvisor(AgentMemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    /**
     * 调用前从记忆存储加载历史消息并按 token 上限自动修剪。
     *
     * @param session 当前会话（通过 sessionId 定位记忆）
     * @return 修剪后的历史消息列表，已按时间正序排列
     */
    public List<ChatMessage> before(AgentSession session) {
        return memoryManager.loadAndTrim(session.getSessionId());
    }

    /**
     * 调用后将本轮用户消息写入记忆存储。空消息（null 或空白）会被忽略。
     *
     * @param request 本轮请求对象
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
     * 将助手回复持久化到对话记忆，确保下一轮请求时 LLM 能看到自己的历史回复。
     *
     * @param sessionId 会话标识
     * @param content   助手回复内容；null 或空白时不做写入
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
