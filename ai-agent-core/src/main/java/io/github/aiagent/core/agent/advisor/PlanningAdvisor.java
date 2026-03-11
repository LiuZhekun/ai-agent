package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.AgentAdvisor;
import io.github.aiagent.core.agent.AgentRequest;
import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.model.AgentEvent;
import io.github.aiagent.core.planner.PlanExecutor;
import io.github.aiagent.core.planner.TaskPlan;
import io.github.aiagent.core.planner.TaskPlanner;
import io.github.aiagent.core.tool.AgentToolCallbackProvider;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 任务规划 Advisor —— 提供"先规划、再执行"的复杂任务处理能力。
 *
 * <h3>在 Advisor 链中的位置</h3>
 * <p>{@code @Order(30)}，位于 {@link ClarificationAdvisor}（Order 20）之后。
 * 仅当参数校验通过后，才有必要进入规划阶段。</p>
 *
 * <h3>启发式规划触发</h3>
 * <p>当前使用简单的启发式规则（消息长度 &gt; 12 或包含"然后"关键词）判断是否启动规划，
 * 后续可替换为基于 LLM 的意图分类。</p>
 *
 * @see io.github.aiagent.core.planner.TaskPlanner
 * @see io.github.aiagent.core.planner.PlanExecutor
 */
@Component
@Order(30)
public class PlanningAdvisor implements AgentAdvisor {

    private final TaskPlanner taskPlanner;
    private final PlanExecutor planExecutor;
    private final AgentToolCallbackProvider toolCallbackProvider;

    public PlanningAdvisor(TaskPlanner taskPlanner,
                           PlanExecutor planExecutor,
                           AgentToolCallbackProvider toolCallbackProvider) {
        this.taskPlanner = taskPlanner;
        this.planExecutor = planExecutor;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * AgentAdvisor 接口实现 —— 在 Advisor 链中自动被 AgentEngine 调用。
     *
     * <p>根据用户消息判断是否需要任务规划，若需要则生成计划并存入 Session。
     * 计划不会在 before 阶段执行，而是在 after 阶段或由 Engine 显式触发执行。</p>
     */
    @Override
    public void before(AgentSession session, AgentRequest request) {
        List<ToolMetadata> tools = toolCallbackProvider.getToolMetadatas();
        before(request, session, tools);
    }

    /**
     * 根据用户消息判断是否需要进行任务规划（支持直接调用的重载方法）。
     *
     * @param request 本轮请求
     * @param session 当前会话上下文
     * @param tools   可用工具元数据列表（供 Planner 决定分步方案）
     * @return 生成的任务计划；若本轮不需要规划则返回 {@code null}
     */
    public TaskPlan before(AgentRequest request, AgentSession session, List<ToolMetadata> tools) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return session.getCurrentPlan();
        }
        boolean needsPlan = request.getMessage().length() > 12 || request.getMessage().contains("然后");
        if (!needsPlan) {
            return null;
        }
        TaskPlan plan = taskPlanner.plan(request.getMessage(), tools);
        session.setCurrentPlan(plan);
        return plan;
    }

    /**
     * 若当前会话中已有待执行计划，则启动分步执行并返回事件流。
     *
     * @param session 当前会话上下文
     * @return 执行过程中产生的事件流；无计划时返回 {@link Flux#empty()}
     */
    public Flux<AgentEvent> executeIfPresent(AgentSession session) {
        if (session.getCurrentPlan() == null) {
            return Flux.empty();
        }
        return planExecutor.execute(session.getCurrentPlan(), session);
    }
}
