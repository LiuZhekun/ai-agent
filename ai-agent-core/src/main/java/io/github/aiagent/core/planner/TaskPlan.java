package io.github.aiagent.core.planner;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务计划模型。
 */
public class TaskPlan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String planId;
    private int version = 1;
    private List<TaskStep> steps = new ArrayList<>();
    private String originalUserIntent;
    private Instant createdAt = Instant.now();

    /**
     * 返回所有依赖满足且待执行的步骤。
     */
    public List<TaskStep> getNextExecutableSteps() {
        return steps.stream()
                .filter(step -> step.getStatus() == TaskStep.StepStatus.PENDING)
                .filter(this::dependenciesSatisfied)
                .collect(Collectors.toList());
    }

    public void markStepDone(int stepId, Object result) {
        TaskStep step = findStep(stepId);
        step.setStatus(TaskStep.StepStatus.DONE);
        step.setResult(result);
    }

    public void markStepFailed(int stepId, String error) {
        TaskStep step = findStep(stepId);
        step.setStatus(TaskStep.StepStatus.FAILED);
        step.setErrorMessage(error);
    }

    public boolean isComplete() {
        return steps.stream()
                .allMatch(s -> s.getStatus() == TaskStep.StepStatus.DONE || s.getStatus() == TaskStep.StepStatus.SKIPPED);
    }

    private boolean dependenciesSatisfied(TaskStep step) {
        if (step.getDependencies() == null || step.getDependencies().isEmpty()) {
            return true;
        }
        return step.getDependencies().stream()
                .map(this::findStep)
                .allMatch(dep -> dep.getStatus() == TaskStep.StepStatus.DONE);
    }

    private TaskStep findStep(int stepId) {
        return steps.stream()
                .filter(s -> s.getStepId() == stepId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<TaskStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TaskStep> steps) {
        this.steps = steps;
    }

    public String getOriginalUserIntent() {
        return originalUserIntent;
    }

    public void setOriginalUserIntent(String originalUserIntent) {
        this.originalUserIntent = originalUserIntent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(planId, version);
    }
}
