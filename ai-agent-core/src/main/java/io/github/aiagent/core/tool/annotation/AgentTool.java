package io.github.aiagent.core.tool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent 工具分组注解 —— 用于标记和描述工具 Bean 的元数据。
 *
 * <h3>重要说明</h3>
 * <p>该注解<b>仅用于元数据分组</b>，不负责工具方法的注册。
 * 实际的工具方法注册依赖 Spring AI 的 {@code @Tool} 注解。
 * 两者的关系如下：</p>
 * <ul>
 *   <li>{@code @AgentTool}（类级别）—— 标记 Bean 为 Agent 工具，提供分组名称、描述和风险等级；</li>
 *   <li>{@code @Tool}（方法级别）—— 由 Spring AI 识别并注册为可被 LLM 调用的工具方法。</li>
 * </ul>
 *
 * <p>该注解同时包含 {@link Component @Component} 元注解，
 * 因此标注了 {@code @AgentTool} 的类会自动注册为 Spring Bean，
 * 并被 {@link io.github.aiagent.core.tool.AgentToolCallbackProvider} 扫描发现。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @AgentTool(name = "用户管理", description = "用户相关操作", riskLevel = RiskLevel.MEDIUM)
 * public class UserTool {
 *     @Tool(description = "根据ID查询用户")
 *     public User findById(String userId) { ... }
 * }
 * }</pre>
 *
 * @see io.github.aiagent.core.tool.AgentToolCallbackProvider
 * @see io.github.aiagent.core.tool.ToolMetadata
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface AgentTool {

    String name();

    String description() default "";

    RiskLevel riskLevel() default RiskLevel.LOW;

    enum RiskLevel {
        /** 低风险操作。 */
        LOW,
        /** 中风险操作。 */
        MEDIUM,
        /** 高风险操作。 */
        HIGH
    }
}
