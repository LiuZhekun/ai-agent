package io.github.aiagent.core.planner;

/**
 * 计划修订策略。
 */
public enum PlanRevisionStrategy {
    /** 重新规划。 */
    REPLAN,
    /** 跳过当前步骤。 */
    SKIP,
    /** 中止执行。 */
    ABORT
}
