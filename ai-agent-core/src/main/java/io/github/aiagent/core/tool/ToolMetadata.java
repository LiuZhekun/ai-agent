package io.github.aiagent.core.tool;

import io.github.aiagent.core.tool.annotation.AgentTool;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具元数据 —— 描述一个 Agent 工具的静态信息。
 *
 * <p>由 {@link AgentToolCallbackProvider} 在扫描 {@link io.github.aiagent.core.tool.annotation.AgentTool @AgentTool}
 * Bean 时构建，涵盖工具分组、名称、描述、参数列表和风险等级等信息。</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>{@link io.github.aiagent.core.agent.advisor.ClarificationAdvisor} —— 根据参数列表进行槽位完整性检查；</li>
 *   <li>{@link io.github.aiagent.core.agent.advisor.PlanningAdvisor} —— 根据工具能力进行任务分解；</li>
 *   <li>管理端点 —— 向运维展示已注册工具清单。</li>
 * </ul>
 *
 * <p>注意：该类与 Spring AI 的 {@code org.springframework.ai.tool.metadata.ToolMetadata} 不同，
 * 前者是框架内部的元数据模型，后者是 Spring AI 的 SPI 接口。命名空间通过包路径区分。</p>
 *
 * @see AgentToolCallbackProvider
 */
public class ToolMetadata implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String groupName;
    private String groupDescription;
    private String toolName;
    private String toolDescription;
    private List<ParamInfo> parameters = new ArrayList<>();
    private AgentTool.RiskLevel riskLevel = AgentTool.RiskLevel.LOW;
    private Class<?> beanClass;

    /**
     * 单个工具参数的元信息，包含名称、类型、描述和是否必填。
     * 供 {@link io.github.aiagent.core.agent.advisor.ClarificationAdvisor} 进行槽位检查。
     */
    public static class ParamInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String name;
        private String type;
        private String description;
        private boolean required;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public void setToolDescription(String toolDescription) {
        this.toolDescription = toolDescription;
    }

    public List<ParamInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParamInfo> parameters) {
        this.parameters = parameters;
    }

    public AgentTool.RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(AgentTool.RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }
}
