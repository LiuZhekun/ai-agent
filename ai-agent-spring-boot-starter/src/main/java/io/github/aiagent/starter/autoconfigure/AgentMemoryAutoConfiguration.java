package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 记忆系统自动装配 —— 预留的扩展点。
 * <p>
 * 当前阶段，会话记忆相关 Bean（如 ChatMemory、MessageWindowChatMemory 等）
 * 由 {@code ai-agent-core} 模块内的组件直接创建和管理，本配置类暂为空壳。
 * <p>
 * 后续如需将记忆策略提升为可配置的 Starter 级 Bean（如外置 Redis / 向量记忆），
 * 可在此处添加条件化的 Bean 定义。
 */
@AutoConfiguration
public class AgentMemoryAutoConfiguration {
}
