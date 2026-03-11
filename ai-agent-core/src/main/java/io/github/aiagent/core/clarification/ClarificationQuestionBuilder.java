package io.github.aiagent.core.clarification;

import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 澄清问题构建器，负责将缺失的槽位状态转化为面向用户的追问问题。
 * <p>
 * 构建策略：
 * <ul>
 *   <li>优先追问必填（required）且置信度最低的槽位</li>
 *   <li>每次最多生成 {@code batchSize} 个问题，避免一次性向用户提出过多问题影响体验</li>
 * </ul>
 *
 * @see ClarificationManager 调用本构建器生成问题
 */
@Component
public class ClarificationQuestionBuilder {

    /**
     * 根据缺失槽位列表构建追问问题。
     * <p>
     * 排序规则：required 优先 → 置信度升序（最不确定的排在前面）。
     *
     * @param missingSlots 状态为非 FILLED 的槽位列表
     * @param batchSize    本次最多生成的问题数量
     * @return 追问问题列表，按优先级排序
     */
    public List<ClarificationQuestion> build(List<SlotFillingState> missingSlots, int batchSize) {
        return missingSlots.stream()
                .sorted(Comparator.comparing(SlotFillingState::isRequired).reversed()
                        .thenComparing(SlotFillingState::getConfidence))
                .limit(batchSize)
                .map(slot -> {
                    ClarificationQuestion q = new ClarificationQuestion();
                    q.setSlotName(slot.getSlotName());
                    q.setQuestion("请补充参数：" + slot.getSlotName());
                    q.setRequired(slot.isRequired());
                    return q;
                })
                .collect(Collectors.toList());
    }

    /**
     * 澄清问题数据对象，封装一个面向用户的追问问题。
     * <p>
     * {@code options} 字段预留用于选择题形式的追问（如枚举类型参数），
     * 当前默认生成开放式问题。
     */
    public static class ClarificationQuestion implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String slotName;
        private String question;
        private List<String> options;
        private boolean required;

        public String getSlotName() {
            return slotName;
        }

        public void setSlotName(String slotName) {
            this.slotName = slotName;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}
