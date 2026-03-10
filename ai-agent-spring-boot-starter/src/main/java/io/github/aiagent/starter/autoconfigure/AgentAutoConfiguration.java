package io.github.aiagent.starter.autoconfigure;

import io.github.aiagent.starter.properties.AgentProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * 核心自动装配入口。
 * 扫描 core 包下所有组件（Engine/Advisor/Memory/Tool/Formatter/Prompt）。
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentProperties.class)
@ComponentScan(basePackages = "io.github.aiagent.core")
public class AgentAutoConfiguration {
}
