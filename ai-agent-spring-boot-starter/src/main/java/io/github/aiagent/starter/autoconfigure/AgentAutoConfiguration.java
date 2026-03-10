package io.github.aiagent.starter.autoconfigure;

import io.github.aiagent.starter.properties.AgentProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 核心自动装配入口。
 * 该类本身不声明 Bean，主要用于开启统一配置属性绑定与模块化装配起点。
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {
}
