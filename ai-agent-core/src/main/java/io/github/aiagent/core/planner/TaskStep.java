package io.github.aiagent.core.planner;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务步骤模型。
 */
public class TaskStep implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int stepId;
    private String toolName;
    private String description;
    private Map<String, Object> parameters;
    private List<Integer> dependencies = new ArrayList<>();
    private StepStatus status = StepStatus.PENDING;
    private Object result;
    private String errorMessage;

    public enum StepStatus {
        /** 待执行。 */
        PENDING,
        /** 执行中。 */
        RUNNING,
        /** 执行完成。 */
        DONE,
        /** 执行失败。 */
        FAILED,
        /** 已跳过。 */
        SKIPPED
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<Integer> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Integer> dependencies) {
        this.dependencies = dependencies;
    }

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
