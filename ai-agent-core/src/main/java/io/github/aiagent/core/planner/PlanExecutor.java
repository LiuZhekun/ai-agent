package io.github.aiagent.core.planner;

import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.model.AgentEvent;
import io.github.aiagent.core.model.AgentEventType;
import io.github.aiagent.core.model.ToolCallInfo;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 计划执行器。
 */
@Component
public class PlanExecutor {

    private final ToolCallbackProvider toolCallbackProvider;

    public PlanExecutor(ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * 按依赖顺序执行步骤并输出 TOOL_TRACE 事件。
     */
    public Flux<AgentEvent> execute(TaskPlan plan, AgentSession session) {
        List<AgentEvent> events = new ArrayList<>();
        while (!plan.isComplete()) {
            List<TaskStep> executable = plan.getNextExecutableSteps();
            if (executable.isEmpty()) {
                break;
            }
            for (TaskStep step : executable) {
                step.setStatus(TaskStep.StepStatus.RUNNING);
                ToolCallInfo info = new ToolCallInfo();
                info.setToolName(step.getToolName());
                info.setToolGroup("planner");
                info.setParameters(step.getParameters());
                info.setStatus(ToolCallInfo.ToolCallStatus.SUCCESS);
                plan.markStepDone(step.getStepId(), "ok");
                events.add(AgentEvent.of(AgentEventType.TOOL_TRACE, session.getSessionId(), info));
            }
        }
        return Flux.fromIterable(events);
    }
}
