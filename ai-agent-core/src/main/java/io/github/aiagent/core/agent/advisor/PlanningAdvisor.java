package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.AgentRequest;
import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.planner.PlanExecutor;
import io.github.aiagent.core.planner.TaskPlan;
import io.github.aiagent.core.planner.TaskPlanner;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 任务规划 Advisor。
 * <p>
 * 说明：该类提供了"先规划再执行"的能力骨架，属于可渐进接入的增强链路。
 * 当前主对话流程以直接问答为主，因此这里的方法在默认链路中调用较少。
 */
@Component
@Order(30)
public class PlanningAdvisor {

    private final TaskPlanner taskPlanner;
    private final PlanExecutor planExecutor;

    public PlanningAdvisor(TaskPlanner taskPlanner, PlanExecutor planExecutor) {
        this.taskPlanner = taskPlanner;
        this.planExecutor = planExecutor;
    }

    /**
     * 根据请求判断是否需要规划，并写入会话计划。
     * 返回 null 表示本轮无需进入规划模式。
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
     * 若已有计划则继续执行。
     * 这是一个显式执行入口，便于后续在 Controller/Engine 中按需接入。
     */
    public Flux<io.github.aiagent.core.model.AgentEvent> executeIfPresent(AgentSession session) {
        if (session.getCurrentPlan() == null) {
            return Flux.empty();
        }
        return planExecutor.execute(session.getCurrentPlan(), session);
    }
}
