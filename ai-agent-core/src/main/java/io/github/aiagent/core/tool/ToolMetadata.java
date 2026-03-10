package io.github.aiagent.core.tool;

import io.github.aiagent.core.tool.annotation.AgentTool;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具元数据定义。
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
     * 参数元信息。
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
