package io.github.aiagent.core.clarification;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单个参数槽位的填充状态，是澄清子系统中 Slot-Filling 工作流的核心数据模型。
 * <p>
 * 每个槽位跟踪以下信息：
 * <ul>
 *   <li>{@code slotName} —— 参数名称，对应工具定义中的参数 key</li>
 *   <li>{@code required} —— 是否为必填参数</li>
 *   <li>{@code value} —— 当前填充值（可能为 null）</li>
 *   <li>{@code confidence} —— 填充置信度（0.0~1.0），用于判断是否需要追问确认</li>
 *   <li>{@code status} —— 填充状态（已填充 / 缺失 / 存在歧义）</li>
 * </ul>
 *
 * @see ClarificationManager#check 创建并评估槽位状态的入口
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

    /**
     * 判断当前槽位是否已满足要求：状态为 FILLED 且置信度超过给定阈值。
     *
     * @param threshold 置信度阈值（0.0~1.0）
     * @return 满足条件返回 true
     */
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
