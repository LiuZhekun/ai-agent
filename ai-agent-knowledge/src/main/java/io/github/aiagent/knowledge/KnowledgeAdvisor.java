package io.github.aiagent.knowledge;

import io.github.aiagent.core.agent.AgentSession;
import org.springframework.stereotype.Component;

/**
 * 知识注入 Advisor。
 */
@Component
public class KnowledgeAdvisor {

    private final KnowledgeManager knowledgeManager;

    public KnowledgeAdvisor(KnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }

    /**
     * 注入知识提示到 session metadata，供系统 Prompt 拼装。
     */
    public void before(AgentSession session) {
        before(session, "");
    }

    /**
     * 根据用户 query 注入知识提示（含 L3 RAG 引用上下文）。
     */
    public void before(AgentSession session, String query) {
        session.getMetadata().put("knowledgeSnippets", knowledgeManager.getKnowledgePrompt(query));
    }
}
