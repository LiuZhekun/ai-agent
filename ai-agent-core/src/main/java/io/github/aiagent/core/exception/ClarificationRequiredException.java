package io.github.aiagent.core.exception;

import java.io.Serial;
import java.util.List;

/**
 * 需要澄清异常。
 * 触发条件：关键参数缺失或歧义，系统无法安全执行下一步动作。
 * 恢复建议：根据 missingSlots 补全参数后重试当前请求。
 */
public class ClarificationRequiredException extends AgentException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String clarificationId;
    private final List<String> missingSlots;

    /**
     * 构造澄清中断异常。
     *
     * @param clarificationId 澄清请求 ID
     * @param missingSlots    缺失槽位列表
     */
    public ClarificationRequiredException(String clarificationId, List<String> missingSlots) {
        super("CLARIFICATION_REQUIRED", "Clarification required: " + clarificationId, true);
        this.clarificationId = clarificationId;
        this.missingSlots = missingSlots;
    }

    public String getClarificationId() {
        return clarificationId;
    }

    public List<String> getMissingSlots() {
        return missingSlots;
    }
}
