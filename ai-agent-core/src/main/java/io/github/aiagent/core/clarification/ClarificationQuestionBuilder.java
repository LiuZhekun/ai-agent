package io.github.aiagent.core.clarification;

import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 澄清问题构建器。
 */
@Component
public class ClarificationQuestionBuilder {

    /**
     * 构建待追问问题，优先 required 且低置信度。
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
     * 澄清问题对象。
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
