package io.github.aiagent.core.clarification;

import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 澄清管理器，维护槽位缺失检查与轮次控制。
 */
@Component
public class ClarificationManager {

    private final ClarificationPolicy policy;
    private final ClarificationQuestionBuilder questionBuilder;
    private final Map<String, Integer> rounds = new ConcurrentHashMap<>();

    public ClarificationManager(ClarificationPolicy policy, ClarificationQuestionBuilder questionBuilder) {
        this.policy = policy;
        this.questionBuilder = questionBuilder;
    }

    /**
     * 检查是否需要澄清。
     */
    public ClarificationResult check(AgentSession session, List<ToolMetadata> targetTools, Map<String, Object> extractedParams) {
        List<SlotFillingState> states = new ArrayList<>();
        for (Map.Entry<String, Object> entry : extractedParams.entrySet()) {
            SlotFillingState state = new SlotFillingState();
            state.setSlotName(entry.getKey());
            state.setRequired(true);
            state.setValue(entry.getValue());
            boolean missing = entry.getValue() == null || String.valueOf(entry.getValue()).isBlank();
            state.setStatus(missing ? SlotFillingState.SlotStatus.MISSING : SlotFillingState.SlotStatus.FILLED);
            state.setConfidence(missing ? 0.0 : 1.0);
            states.add(state);
        }
        List<SlotFillingState> missingSlots = states.stream()
                .filter(s -> s.getStatus() != SlotFillingState.SlotStatus.FILLED)
                .toList();
        ClarificationResult result = new ClarificationResult();
        result.setSlotStates(states);
        if (missingSlots.isEmpty()) {
            result.setNeedsClarification(false);
            result.setQuestions(List.of());
            return result;
        }
        int currentRound = rounds.compute(session.getSessionId(), (k, v) -> v == null ? 1 : v + 1);
        if (currentRound > policy.getMaxRounds()) {
            result.setNeedsClarification(false);
            result.setQuestions(List.of());
            return result;
        }
        result.setNeedsClarification(true);
        result.setQuestions(questionBuilder.build(missingSlots, policy.getAskBatchSize()));
        return result;
    }

    /**
     * 合并用户补充答案。
     */
    public void mergeSlotAnswers(AgentSession session, Map<String, Object> answers) {
        session.getMetadata().put("slotAnswers", answers);
    }

    /**
     * 澄清检查结果。
     */
    public static class ClarificationResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private boolean needsClarification;
        private List<ClarificationQuestionBuilder.ClarificationQuestion> questions;
        private List<SlotFillingState> slotStates;

        public boolean isNeedsClarification() {
            return needsClarification;
        }

        public void setNeedsClarification(boolean needsClarification) {
            this.needsClarification = needsClarification;
        }

        public List<ClarificationQuestionBuilder.ClarificationQuestion> getQuestions() {
            return questions;
        }

        public void setQuestions(List<ClarificationQuestionBuilder.ClarificationQuestion> questions) {
            this.questions = questions;
        }

        public List<SlotFillingState> getSlotStates() {
            return slotStates;
        }

        public void setSlotStates(List<SlotFillingState> slotStates) {
            this.slotStates = slotStates;
        }
    }
}
