package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * 向量模块自动装配，classpath 存在 VectorSyncService 时扫描向量化组件。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.vectorizer.VectorSyncService")
@ComponentScan(basePackages = "io.github.aiagent.vectorizer")
public class AgentVectorAutoConfiguration {
}
