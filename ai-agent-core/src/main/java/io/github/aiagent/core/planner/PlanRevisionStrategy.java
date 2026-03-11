package io.github.aiagent.core.planner;

/**
 * 计划修订策略枚举，定义当步骤执行失败时如何处理后续流程。
 * <p>
 * 在 {@link PlanExecutor} 检测到步骤失败时，根据此策略决定下一步行为。
 * 策略的选择可以由配置驱动，也可以由 LLM 根据错误类型动态决定。
 *
 * @see TaskPlanner#revise(TaskPlan, TaskStep, String) 在 REPLAN 策略下调用
 */
public enum PlanRevisionStrategy {
    /** 丢弃当前计划，由 {@link TaskPlanner} 重新生成一个修订后的计划。 */
    REPLAN,
    /** 跳过当前失败步骤，继续执行后续步骤。 */
    SKIP,
    /** 中止整个计划执行，向用户报告失败。 */
    ABORT
}
