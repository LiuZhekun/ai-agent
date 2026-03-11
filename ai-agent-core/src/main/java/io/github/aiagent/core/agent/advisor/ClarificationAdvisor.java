package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.AgentAdvisor;
import io.github.aiagent.core.agent.AgentRequest;
import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.clarification.ClarificationManager;
import io.github.aiagent.core.clarification.SlotFillingState;
import io.github.aiagent.core.exception.ClarificationRequiredException;
import io.github.aiagent.core.tool.AgentToolCallbackProvider;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 澄清 Advisor —— 在工具调用前检查必填参数是否齐全。
 *
 * <h3>在 Advisor 链中的位置</h3>
 * <p>{@code @Order(20)}，位于 {@link MemoryAdvisor}（Order 10）之后。
 * 这样做是因为澄清检查可能依赖从记忆中恢复的上下文信息。</p>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>若请求中携带 slotAnswers（用户对上一轮追问的回答），先合并到会话上下文；</li>
 *   <li>从 {@link AgentToolCallbackProvider} 获取已注册的工具元数据列表；</li>
 *   <li>委托 {@link ClarificationManager#check} 执行槽位填充状态检查；</li>
 *   <li>若存在未填充的必填槽位，抛出 {@link ClarificationRequiredException}，
 *       其中包含唯一的 clarificationId 和缺失槽位列表；</li>
 *   <li>{@link io.github.aiagent.core.agent.AgentEngine} 捕获该异常后，
 *       向前端发送 {@code CLARIFICATION_REQUIRED} 事件，引导用户补全信息。</li>
 * </ol>
 *
 * @see ClarificationManager
 * @see ClarificationRequiredException
 */
@Component
@Order(20)
public class ClarificationAdvisor implements AgentAdvisor {

    private final ClarificationManager clarificationManager;
    private final AgentToolCallbackProvider toolCallbackProvider;

    public ClarificationAdvisor(ClarificationManager clarificationManager,
                                AgentToolCallbackProvider toolCallbackProvider) {
        this.clarificationManager = clarificationManager;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * AgentAdvisor 接口实现 —— 在 Advisor 链中自动被 AgentEngine 调用。
     *
     * <p>若请求携带 slotAnswers（用户对追问的回答），先合并到会话上下文，
     * 然后执行槽位完整性校验。槽位不完整时抛出 {@link ClarificationRequiredException}。</p>
     */
    @Override
    public void before(AgentSession session, AgentRequest request) {
        if (request.getSlotAnswers() != null && !request.getSlotAnswers().isEmpty()) {
            clarificationManager.mergeSlotAnswers(session, request.getSlotAnswers());
        }

        List<ToolMetadata> tools = toolCallbackProvider.getToolMetadatas();
        Map<String, Object> extractedParams = request.getSlotAnswers() != null
                ? request.getSlotAnswers()
                : Collections.emptyMap();

        before(session, tools, extractedParams);
    }

    /**
     * 对目标工具的必填参数进行完整性校验（支持直接调用的重载方法）。
     *
     * @param session         当前会话上下文
     * @param targetTools     本轮可能被调用的工具元数据列表
     * @param extractedParams 从用户消息中已提取的参数键值对
     * @throws ClarificationRequiredException 若存在未填充的必填槽位
     */
    public void before(AgentSession session, List<ToolMetadata> targetTools, Map<String, Object> extractedParams) {
        ClarificationManager.ClarificationResult result = clarificationManager.check(session, targetTools, extractedParams);
        if (!result.isNeedsClarification()) {
            return;
        }
        List<String> missingSlots = result.getSlotStates().stream()
                .filter(s -> s.getStatus() != SlotFillingState.SlotStatus.FILLED)
                .map(SlotFillingState::getSlotName)
                .toList();
        throw new ClarificationRequiredException(UUID.randomUUID().toString(), missingSlots);
    }
}
