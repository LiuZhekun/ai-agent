package io.github.aiagent.starter.autoconfigure;

import io.github.aiagent.core.metrics.AgentMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * 指标与审计自动装配。
 * 仅当 MeterRegistry 可用时注册 AgentMetrics Bean。
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
