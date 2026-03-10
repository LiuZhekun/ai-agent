package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Web 接口自动装配，扫描 Controller 和全局异常处理。
 */
@AutoConfiguration
@ComponentScan(basePackages = "io.github.aiagent.starter.endpoint")
public class AgentWebAutoConfiguration {
}
