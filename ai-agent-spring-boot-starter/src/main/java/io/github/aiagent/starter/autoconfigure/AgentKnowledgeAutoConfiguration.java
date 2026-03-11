package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * 知识模块自动装配 —— 按需激活。
 * <p>
 * 仅当 classpath 中存在 {@code io.github.aiagent.knowledge.KnowledgeManager} 时生效
 * （即项目引入了 {@code ai-agent-knowledge} 依赖）。
 * 激活后会扫描 {@code io.github.aiagent.knowledge} 包，自动注册
 * Schema 发现、安全 SQL 执行等知识库相关组件。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.knowledge.KnowledgeManager")
@ComponentScan(basePackages = "io.github.aiagent.knowledge")
public class AgentKnowledgeAutoConfiguration {
}
