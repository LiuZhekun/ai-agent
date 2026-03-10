package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * 知识模块自动装配。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.knowledge.KnowledgeManager")
@ComponentScan(basePackages = "io.github.aiagent.knowledge")
public class AgentKnowledgeAutoConfiguration {
}
