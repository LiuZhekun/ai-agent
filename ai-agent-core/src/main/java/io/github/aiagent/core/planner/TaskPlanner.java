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
 * 任务规划器。
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
     * 生成任务计划。
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
     * 失败后修订计划。
     * <p>
     * 当前版本主要用于保留"失败自动重规划"扩展点，默认流程中尚未强制调用。
     * 后续可在 PlanExecutor 捕获失败后调用本方法生成新计划。
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
