package io.github.aiagent.core.planner;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务计划数据模型，由 {@link TaskPlanner} 生成，由 {@link PlanExecutor} 执行。
 * <p>
 * 一个计划包含：
 * <ul>
 *   <li>{@code planId} —— 计划唯一标识</li>
 *   <li>{@code version} —— 版本号，每次修订（revise）递增</li>
 *   <li>{@code steps} —— 有序的步骤列表，步骤之间可通过 {@link TaskStep#getDependencies()} 声明依赖关系</li>
 *   <li>{@code originalUserIntent} —— 原始用户意图，用于修订时参考</li>
 * </ul>
 * <p>
 * 执行模型：{@link #getNextExecutableSteps()} 返回所有依赖已满足且状态为 PENDING 的步骤，
 * 支持在依赖允许的范围内并行执行多个步骤。
 *
 * @see TaskStep 步骤模型
 * @see TaskPlanner 计划生成器
 * @see PlanExecutor 计划执行器
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
     * 返回当前可执行的步骤列表——即状态为 PENDING 且所有依赖步骤已完成（DONE）的步骤。
     * <p>
     * 返回的步骤之间互不依赖，理论上可以并行执行。
     *
     * @return 可执行步骤列表；所有步骤都已完成或存在阻塞依赖时返回空列表
     */
    public List<TaskStep> getNextExecutableSteps() {
        return steps.stream()
                .filter(step -> step.getStatus() == TaskStep.StepStatus.PENDING)
                .filter(this::dependenciesSatisfied)
                .collect(Collectors.toList());
    }

    /**
     * 标记指定步骤为已完成，并记录执行结果。
     *
     * @param stepId 步骤 ID
     * @param result 执行结果
     */
    public void markStepDone(int stepId, Object result) {
        TaskStep step = findStep(stepId);
        step.setStatus(TaskStep.StepStatus.DONE);
        step.setResult(result);
    }

    /**
     * 标记指定步骤为失败，并记录错误信息。
     *
     * @param stepId 步骤 ID
     * @param error  错误描述
     */
    public void markStepFailed(int stepId, String error) {
        TaskStep step = findStep(stepId);
        step.setStatus(TaskStep.StepStatus.FAILED);
        step.setErrorMessage(error);
    }

    /**
     * 判断计划是否已全部完成（所有步骤状态为 DONE 或 SKIPPED）。
     *
     * @return 全部完成返回 true
     */
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
