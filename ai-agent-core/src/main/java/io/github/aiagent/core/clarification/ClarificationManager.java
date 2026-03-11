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
 * 澄清管理器，负责在工具调用前检测参数缺失并引导用户补全（Slot-Filling 工作流）。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>Agent 从用户意图中提取出参数（extractedParams），其中可能有部分参数值为 null/空白</li>
 *   <li>{@link #check(AgentSession, List, Map)} 将每个参数包装为 {@link SlotFillingState}，
 *       标记为 FILLED 或 MISSING</li>
 *   <li>如果存在 MISSING 槽位，通过 {@link ClarificationQuestionBuilder} 生成追问问题，
 *       返回给前端让用户补充</li>
 *   <li>用户回答后，通过 {@link #mergeSlotAnswers(AgentSession, Map)} 合并答案到会话上下文</li>
 *   <li>重复以上步骤直到所有槽位填满，或达到 {@link ClarificationPolicy#getMaxRounds()} 最大轮次上限</li>
 * </ol>
 * <p>
 * 轮次控制使用 {@link ConcurrentHashMap} 按 sessionId 跟踪，超过最大轮次后不再追问，
 * 具体降级行为由 {@link ClarificationPolicy.OnMaxRoundsExceeded} 决定。
 *
 * @see ClarificationPolicy 澄清策略配置（最大轮次、批次大小等）
 * @see SlotFillingState 槽位填充状态模型
 * @see ClarificationQuestionBuilder 追问问题生成器
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
     * 检查已提取的参数中是否存在缺失槽位，决定是否需要向用户追问。
     * <p>
     * 如果所有参数都已填充或已达最大追问轮次，返回的结果中 {@code needsClarification} 为 false；
     * 否则返回待追问的问题列表。
     *
     * @param session         当前会话，用于跟踪追问轮次
     * @param targetTools     目标工具列表（预留，当前未直接使用，后续可用于按工具 schema 校验参数）
     * @param extractedParams 已从用户意图中提取的参数，值为 null 或空白字符串表示缺失
     * @return 澄清检查结果，包含是否需要追问、问题列表和各槽位状态
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
     * 将用户补充的槽位答案合并到会话元数据中。
     * <p>
     * 合并后的数据存储在 {@code session.metadata["slotAnswers"]} 中，
     * 后续工具调用时可从此处获取完整参数。
     *
     * @param session 当前会话
     * @param answers 用户补充的参数键值对
     */
    public void mergeSlotAnswers(AgentSession session, Map<String, Object> answers) {
        session.getMetadata().put("slotAnswers", answers);
    }

    /**
     * 澄清检查结果，封装了是否需要追问、追问问题列表以及各槽位的当前状态。
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
