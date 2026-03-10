package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * 翻译模块自动装配，classpath 存在 TranslatorRegistry 时扫描翻译组件。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.translator.TranslatorRegistry")
@ComponentScan(basePackages = "io.github.aiagent.translator")
public class AgentTranslatorAutoConfiguration {
}
