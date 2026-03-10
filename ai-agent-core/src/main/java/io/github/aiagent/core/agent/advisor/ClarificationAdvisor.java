package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.clarification.ClarificationManager;
import io.github.aiagent.core.exception.ClarificationRequiredException;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 澄清 Advisor。
 */
@Component
@Order(20)
public class ClarificationAdvisor {

    private final ClarificationManager clarificationManager;

    public ClarificationAdvisor(ClarificationManager clarificationManager) {
        this.clarificationManager = clarificationManager;
    }

    /**
     * 调用前进行参数完整性检查；缺失时抛出澄清中断异常。
     */
    public void before(AgentSession session, List<ToolMetadata> targetTools, Map<String, Object> extractedParams) {
        ClarificationManager.ClarificationResult result = clarificationManager.check(session, targetTools, extractedParams);
        if (!result.isNeedsClarification()) {
            return;
        }
        List<String> missingSlots = result.getSlotStates().stream()
                .filter(s -> s.getStatus() != io.github.aiagent.core.clarification.SlotFillingState.SlotStatus.FILLED)
                .map(io.github.aiagent.core.clarification.SlotFillingState::getSlotName)
                .toList();
        throw new ClarificationRequiredException(UUID.randomUUID().toString(), missingSlots);
    }
}
