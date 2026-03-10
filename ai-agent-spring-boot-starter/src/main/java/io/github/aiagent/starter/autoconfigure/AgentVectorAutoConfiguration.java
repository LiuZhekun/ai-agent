package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * 向量模块自动装配。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.vectorizer.VectorSyncService")
public class AgentVectorAutoConfiguration {
}
