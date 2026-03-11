package io.github.aiagent.core.planner;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务计划中的单个执行步骤，是 {@link TaskPlan} 的组成元素。
 * <p>
 * 每个步骤描述了一次工具调用，包含：
 * <ul>
 *   <li>{@code stepId} —— 步骤唯一标识（计划内唯一）</li>
 *   <li>{@code toolName} —— 需要调用的工具名称</li>
 *   <li>{@code parameters} —— 工具调用参数</li>
 *   <li>{@code dependencies} —— 依赖的前置步骤 ID 列表，所有依赖完成后本步骤才可执行</li>
 *   <li>{@code status} —— 当前执行状态，参见 {@link StepStatus}</li>
 * </ul>
 *
 * @see TaskPlan 所属的计划模型
 * @see StepStatus 步骤状态枚举
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
