package io.github.aiagent.starter.autoconfigure;

import io.github.aiagent.starter.properties.AgentProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI Agent 核心自动装配入口 —— Starter 中最先加载的配置类。
 * <p>
 * 职责：
 * <ol>
 *   <li>通过 {@link EnableConfigurationProperties} 绑定 {@link AgentProperties}（前缀 {@code ai.agent.*}）</li>
 *   <li>通过 {@link ComponentScan} 扫描 {@code io.github.aiagent.core} 包，
 *       自动注册核心组件（AgentEngine、Advisor、Memory、Tool、Formatter、Prompt 等）</li>
 * </ol>
 * <p>
 * 其他可选模块（Knowledge、Vector、Translator 等）由同包下对应的条件配置类按需激活。
 *
 * @see AgentProperties
 * @see AgentKnowledgeAutoConfiguration
 * @see AgentVectorAutoConfiguration
 * @see AgentTranslatorAutoConfiguration
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentProperties.class)
@ComponentScan(basePackages = "io.github.aiagent.core")
public class AgentAutoConfiguration {
}
