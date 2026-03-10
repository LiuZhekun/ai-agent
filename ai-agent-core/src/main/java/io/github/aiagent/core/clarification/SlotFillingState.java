package io.github.aiagent.core.clarification;

import java.io.Serial;
import java.io.Serializable;

/**
 * 槽位填充状态。
 */
public class SlotFillingState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String slotName;
    private boolean required;
    private Object value;
    private double confidence;
    private SlotStatus status;

    public enum SlotStatus {
        /** 已完成填充。 */
        FILLED,
        /** 缺失待补充。 */
        MISSING,
        /** 存在歧义待确认。 */
        AMBIGUOUS
    }

    public boolean isSatisfied(double threshold) {
        return status == SlotStatus.FILLED && confidence > threshold;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
}
