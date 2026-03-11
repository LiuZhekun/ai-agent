package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * 翻译模块自动装配 —— 按需激活。
 * <p>
 * 仅当 classpath 中存在 {@code io.github.aiagent.translator.TranslatorRegistry} 时生效
 * （即项目引入了 {@code ai-agent-translator} 依赖）。
 * 激活后会扫描 {@code io.github.aiagent.translator} 包，自动注册
 * 翻译器、注册中心、装饰器拦截器及 LLM 翻译工具等组件。
 *
 * @see AgentAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.github.aiagent.translator.TranslatorRegistry")
@ComponentScan(basePackages = "io.github.aiagent.translator")
public class AgentTranslatorAutoConfiguration {
}
