package io.github.aiagent.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Demo 业务配置。
 */
@Configuration
public class DemoAgentConfig {

    @Bean
    @ConfigurationProperties(prefix = "ai.agent.demo")
    public DemoAgentProperties demoAgentProperties() {
        return new DemoAgentProperties();
    }

    @Bean("demoToolWhitelist")
    public Set<String> demoToolWhitelist(DemoAgentProperties properties) {
        return new LinkedHashSet<>(properties.getToolWhitelist());
    }

    @Bean
    public Clock demoClock() {
        return Clock.systemDefaultZone();
    }

    public static class DemoAgentProperties {
        private boolean enableReadonlyGuard = true;
        private List<String> toolWhitelist = List.of("user-tools", "department-tools", "dict-tools");
        private int previewRecordLimit = 20;

        public boolean isEnableReadonlyGuard() {
            return enableReadonlyGuard;
        }

        public void setEnableReadonlyGuard(boolean enableReadonlyGuard) {
            this.enableReadonlyGuard = enableReadonlyGuard;
        }

        public List<String> getToolWhitelist() {
            return toolWhitelist;
        }

        public void setToolWhitelist(List<String> toolWhitelist) {
            this.toolWhitelist = toolWhitelist;
        }

        public int getPreviewRecordLimit() {
            return previewRecordLimit;
        }

        public void setPreviewRecordLimit(int previewRecordLimit) {
            this.previewRecordLimit = previewRecordLimit;
        }
    }
}
