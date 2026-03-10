package io.github.aiagent.knowledge;

import io.github.aiagent.core.agent.AgentAdvisor;
import io.github.aiagent.core.agent.AgentRequest;
import io.github.aiagent.core.agent.AgentSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 知识注入 Advisor，实现 AgentAdvisor 接口，
 * 在每轮对话前将 L0-L3 知识拼接到 session metadata 供系统 Prompt 使用。
 */
@Component
@Order(10)
public class KnowledgeAdvisor implements AgentAdvisor {

    private final KnowledgeManager knowledgeManager;

    public KnowledgeAdvisor(KnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }

    @Override
    public void before(AgentSession session, AgentRequest request) {
        String query = request.getMessage() != null ? request.getMessage() : "";
        session.getMetadata().put("knowledgeSnippets", knowledgeManager.getKnowledgePrompt(query));
    }
}
