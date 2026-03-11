package io.github.aiagent.core.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.model.AgentEvent;
import io.github.aiagent.core.model.AgentEventType;
import io.github.aiagent.core.model.ToolCallInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务计划执行器，负责按依赖顺序逐步执行 {@link TaskPlan} 中的步骤。
 * <p>
 * 执行模型：
 * <ul>
 *   <li>循环调用 {@link TaskPlan#getNextExecutableSteps()} 获取当前可执行步骤</li>
 *   <li>通过 {@link ToolCallbackProvider} 查找对应工具并真正执行调用</li>
 *   <li>对每个步骤生成 {@code TOOL_TRACE} 类型的 {@link AgentEvent}，用于前端展示执行过程</li>
 *   <li>步骤失败时根据 {@link PlanRevisionStrategy} 决定是否跳过或中止</li>
 *   <li>所有步骤完成或无法继续推进时结束执行</li>
 * </ul>
 *
 * @see TaskPlan 计划模型
 * @see TaskPlanner 计划生成器
 * @see PlanRevisionStrategy 失败时的修订策略
 */
@Component
public class PlanExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);

    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;

    public PlanExecutor(ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 按依赖顺序执行计划中的步骤，通过 ToolCallbackProvider 真正调用工具。
     *
     * @param plan    待执行的任务计划
     * @param session 当前 Agent 会话
     * @return 包含每个步骤执行 trace 事件的 {@link Flux}
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

                ToolCallback callback = findToolCallback(step.getToolName());
                if (callback == null) {
                    String error = "Tool not found: " + step.getToolName();
                    log.warn("Plan step {} failed: {}", step.getStepId(), error);
                    plan.markStepFailed(step.getStepId(), error);
                    info.setStatus(ToolCallInfo.ToolCallStatus.FAILED);
                    events.add(AgentEvent.of(AgentEventType.TOOL_TRACE, session.getSessionId(), info));
                    continue;
                }

                try {
                    String input = objectMapper.writeValueAsString(
                            step.getParameters() != null ? step.getParameters() : Map.of());
                    String result = callback.call(input);
                    plan.markStepDone(step.getStepId(), result);
                    info.setStatus(ToolCallInfo.ToolCallStatus.SUCCESS);
                    log.info("Plan step {} completed: tool={}", step.getStepId(), step.getToolName());
                } catch (Exception ex) {
                    String error = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    log.warn("Plan step {} failed: tool={}, error={}", step.getStepId(), step.getToolName(), error);
                    plan.markStepFailed(step.getStepId(), error);
                    info.setStatus(ToolCallInfo.ToolCallStatus.FAILED);
                }

                events.add(AgentEvent.of(AgentEventType.TOOL_TRACE, session.getSessionId(), info));
            }
        }
        return Flux.fromIterable(events);
    }

    /**
     * 在已注册的工具回调中查找与步骤名称匹配的 ToolCallback。
     */
    private ToolCallback findToolCallback(String toolName) {
        if (toolName == null) {
            return null;
        }
        for (ToolCallback callback : toolCallbackProvider.getToolCallbacks()) {
            if (toolName.equals(callback.getToolDefinition().name())) {
                return callback;
            }
        }
        return null;
    }
}
