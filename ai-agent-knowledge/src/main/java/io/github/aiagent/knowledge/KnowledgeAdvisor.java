package io.github.aiagent.knowledge;

import io.github.aiagent.core.agent.AgentAdvisor;
import io.github.aiagent.core.agent.AgentRequest;
import io.github.aiagent.core.agent.AgentSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 知识注入 Advisor —— 在每轮对话前将 L0-L3 知识上下文写入 session metadata。
 * <p>
 * 实现 {@link AgentAdvisor} 接口，通过 {@link #before(AgentSession, AgentRequest)} 回调，
 * 在 LLM 调用之前将 {@link KnowledgeManager} 组装的知识文本写入
 * {@code session.metadata["knowledgeSnippets"]}。系统 Prompt 模板中可通过该 key
 * 引用知识内容，从而让模型在回答时具备业务背景知识。
 * <p>
 * 通过 {@code @Order(10)} 确保知识注入在其他 Advisor（如思考摘要）之前执行，
 * 使后续 Advisor 可以感知到已注入的知识上下文。
 *
 * @see KnowledgeManager
 * @see AgentAdvisor
 */
@Component
@Order(10)
public class KnowledgeAdvisor implements AgentAdvisor {

    private final KnowledgeManager knowledgeManager;

    public KnowledgeAdvisor(KnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }

    /**
     * 对话前回调：提取用户消息作为 RAG 查询条件，组装知识并写入 session metadata。
     *
     * @param session 当前会话上下文，知识文本将写入其 metadata
     * @param request 本轮用户请求，{@code message} 字段用于 L3 RAG 检索
     */
    @Override
    public void before(AgentSession session, AgentRequest request) {
        String query = request.getMessage() != null ? request.getMessage() : "";
        session.getMetadata().put("knowledgeSnippets", knowledgeManager.getKnowledgePrompt(query));
    }
}
