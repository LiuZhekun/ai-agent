package io.github.aiagent.starter.autoconfigure;

import io.github.aiagent.core.metrics.AgentMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * 指标与审计自动装配。
 * <p>
 * 仅当 classpath 中存在 Micrometer 的 {@link MeterRegistry} 时生效。
 * 进一步通过 {@link ConditionalOnBean} 确保容器中已有 {@link MeterRegistry} 实例后，
 * 才注册 {@link AgentMetrics} Bean，用于记录 Agent 级别的调用耗时、token 消耗等指标。
 * <p>
 * 如果项目不依赖 {@code spring-boot-starter-actuator}，本配置类不会激活，
 * 不影响核心功能。
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class AgentMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public AgentMetrics agentMetrics(MeterRegistry meterRegistry) {
        return new AgentMetrics(meterRegistry);
    }
}
