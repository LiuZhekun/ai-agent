package io.github.aiagent.core.tool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent 工具分组注解。
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
