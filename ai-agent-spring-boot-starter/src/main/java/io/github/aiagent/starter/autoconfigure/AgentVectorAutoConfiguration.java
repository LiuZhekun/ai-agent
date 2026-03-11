package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * 向量化模块自动装配 —— 按需激活。
 * <p>
 * 仅当 classpath 中存在 {@code io.github.aiagent.vectorizer.VectorSyncService} 时生效
 * （即项目引入了 {@code ai-agent-vectorizer} 依赖）。
 * 激活后会扫描 {@code io.github.aiagent.vectorizer} 包，自动注册
 * 实体向量同步、定时调度等组件。
 *
 * @see AgentAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.vectorizer.VectorSyncService")
@ComponentScan(basePackages = "io.github.aiagent.vectorizer")
public class AgentVectorAutoConfiguration {
}
