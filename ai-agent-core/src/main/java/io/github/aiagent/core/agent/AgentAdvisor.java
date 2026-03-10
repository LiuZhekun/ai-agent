package io.github.aiagent.core.agent;

/**
 * 可选模块扩展点接口。
 * 实现此接口的 Bean 会被 AgentEngine 自动发现，按 Spring @Order 编排，
 * 在 LLM 调用前后执行（知识注入、参数校验等）。
 */
public interface AgentAdvisor {

    default void before(AgentSession session, AgentRequest request) {}

    default void after(AgentSession session, String response) {}
}
