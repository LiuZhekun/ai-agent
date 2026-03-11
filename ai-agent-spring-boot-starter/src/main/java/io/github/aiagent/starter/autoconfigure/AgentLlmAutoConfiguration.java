package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * LLM 提供商自动装配 —— 预留的扩展点。
 * <p>
 * 当前阶段，LLM 相关的 Bean（ChatModel、EmbeddingModel 等）由 Spring AI 自身的
 * Auto-Configuration 自动创建，本类仅作为占位，未定义额外的 Bean。
 * <p>
 * 后续如需在框架层面统一管理多模型切换、限流、回退等逻辑，可在此处注册自定义 Bean。
 * 仅在 {@code ai.agent.llm.provider} 配置项存在时激活。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "ai.agent.llm", name = "provider")
public class AgentLlmAutoConfiguration {
}
