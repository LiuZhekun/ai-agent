package io.github.aiagent.core.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于 LLM 的任务规划器，负责将用户意图分解为可执行的多步骤计划。
 * <p>
 * 核心思路：将用户的自然语言意图和可用工具列表发送给 LLM，由 LLM 输出结构化的
 * {@link TaskPlan}（JSON 格式），包含有序的 {@link TaskStep} 列表及其依赖关系。
 * <p>
 * 容错设计：
 * <ul>
 *   <li>LLM 输出的 JSON 解析失败时，自动降级为单步兜底计划（{@code fallbackPlan}），
 *       确保流程不会因解析异常而中断</li>
 *   <li>提供 {@link #revise(TaskPlan, TaskStep, String)} 方法作为"失败重规划"扩展点，
 *       供后续 {@link PlanExecutor} 在步骤执行失败时调用</li>
 * </ul>
 *
 * @see TaskPlan 任务计划模型
 * @see PlanExecutor 计划执行器
 */
@Component
public class TaskPlanner {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public TaskPlanner(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 根据用户意图和可用工具列表，调用 LLM 生成结构化的任务计划。
     *
     * @param userIntent     用户的自然语言意图描述
     * @param availableTools 当前可用的工具列表，LLM 会据此规划每个步骤使用哪个工具
     * @return 任务计划；LLM 输出解析失败时返回单步兜底计划
     */
    public TaskPlan plan(String userIntent, List<ToolMetadata> availableTools) {
        String toolNames = availableTools == null ? "" : availableTools.stream().map(ToolMetadata::getToolName).toList().toString();
        String response = chatClient.prompt()
                .user("请根据用户意图输出 JSON TaskPlan。\n意图：" + userIntent + "\n工具：" + toolNames)
                .call()
                .content();
        TaskPlan parsed = tryParse(response);
        if (parsed != null) {
            return parsed;
        }
        return fallbackPlan(userIntent);
    }

    /**
     * 在步骤执行失败后修订计划。
     * <p>
     * 当前版本主要作为"失败自动重规划"的扩展点，默认流程中尚未强制调用。
     * 后续可在 {@link PlanExecutor} 捕获步骤失败后调用本方法，
     * 由 LLM 根据失败信息生成修订后的新计划。
     *
     * @param currentPlan 当前计划
     * @param failedStep  执行失败的步骤
     * @param errorInfo   错误描述信息
     * @return 修订后的新计划；LLM 输出解析失败时返回原计划不变
     */
    public TaskPlan revise(TaskPlan currentPlan, TaskStep failedStep, String errorInfo) {
        String response = chatClient.prompt()
                .user("请修订计划。失败步骤：" + failedStep.getStepId() + "，错误：" + errorInfo)
                .call()
                .content();
        TaskPlan parsed = tryParse(response);
        if (parsed != null) {
            return parsed;
        }
        return currentPlan;
    }

    private TaskPlan tryParse(String response) {
        try {
            return objectMapper.readValue(response, TaskPlan.class);
        } catch (Exception ignore) {
            return null;
        }
    }

    private TaskPlan fallbackPlan(String intent) {
        TaskPlan plan = new TaskPlan();
        plan.setPlanId(UUID.randomUUID().toString());
        plan.setVersion(1);
        plan.setOriginalUserIntent(intent);
        plan.setCreatedAt(Instant.now());
        TaskStep step = new TaskStep();
        step.setStepId(1);
        step.setDescription("根据用户意图进行单步处理");
        step.setToolName("default-tool");
        step.setParameters(new java.util.HashMap<>());
        List<TaskStep> steps = new ArrayList<>();
        steps.add(step);
        plan.setSteps(steps);
        return plan;
    }
}
