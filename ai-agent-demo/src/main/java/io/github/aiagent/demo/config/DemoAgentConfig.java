package io.github.aiagent.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Demo 业务配置 —— 演示如何为业务模块定义可外部化的 Agent 行为参数。
 *
 * <h2>接入要点</h2>
 * <p>
 * 当你需要对 Agent 暴露的工具做业务级别的管控（白名单、只读模式、结果限制等）时，
 * 可以像本类一样将参数抽取为 {@code @ConfigurationProperties}，并通过 yml / 环境变量注入。
 * </p>
 *
 * <h2>配置示例（application.yml）</h2>
 * <pre>{@code
 * ai:
 *   agent:
 *     demo:
 *       enable-readonly-guard: true
 *       tool-whitelist:
 *         - user-tools
 *         - department-tools
 *         - dict-tools
 *       preview-record-limit: 20
 * }</pre>
 */
@Configuration
public class DemoAgentConfig {

    /**
     * 将 {@code ai.agent.demo.*} 配置项绑定到 {@link DemoAgentProperties} 对象上。
     * Spring Boot 会自动完成 yml 属性与 Java 字段的映射。
     */
    @Bean
    @ConfigurationProperties(prefix = "ai.agent.demo")
    public DemoAgentProperties demoAgentProperties() {
        return new DemoAgentProperties();
    }

    /**
     * 工具白名单集合 —— 只有名称在白名单中的 {@code @AgentTool} 才会被 Agent 加载。
     * <p>
     * 白名单中的名称对应 {@code @AgentTool(name = "xxx")} 的 name 值。
     * 如果你新增了自定义工具，记得将其名称加入此列表，否则 Agent 不会调用它。
     */
    @Bean("demoToolWhitelist")
    public Set<String> demoToolWhitelist(DemoAgentProperties properties) {
        return new LinkedHashSet<>(properties.getToolWhitelist());
    }

    /**
     * 提供系统时钟，便于工具层获取当前时间（同时也方便测试时 Mock）。
     */
    @Bean
    public Clock demoClock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Demo 业务属性 —— 通过 {@code ai.agent.demo.*} 配置。
     */
    public static class DemoAgentProperties {
        /** 是否开启只读保护。为 true 时所有写操作需二次确认。 */
        private boolean enableReadonlyGuard = true;
        /** 允许 Agent 调用的工具名称列表，对应 {@code @AgentTool(name)} 的值。 */
        private List<String> toolWhitelist = List.of("user-tools", "department-tools", "dict-tools");
        /** 预览查询结果的最大条数，防止大表全量返回。 */
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
