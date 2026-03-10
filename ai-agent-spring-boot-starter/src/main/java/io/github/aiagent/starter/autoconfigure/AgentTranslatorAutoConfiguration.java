package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * 翻译模块自动装配。
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.translator.TranslatorRegistry")
public class AgentTranslatorAutoConfiguration {
}
