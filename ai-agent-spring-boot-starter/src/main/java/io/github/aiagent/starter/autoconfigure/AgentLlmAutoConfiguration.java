package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * LLM 提供商自动装配。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "ai.agent.llm", name = "provider")
public class AgentLlmAutoConfiguration {
}
