package io.github.aiagent.core.agent;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Agent 请求对象 —— 从前端到 {@link AgentEngine} 的统一入参。
 *
 * <p>一个请求可能携带以下信息之一或组合：</p>
 * <ul>
 *   <li>{@link #message} —— 普通用户消息（最常见场景）；</li>
 *   <li>{@link #slotAnswers} —— 对 {@link io.github.aiagent.core.agent.advisor.ClarificationAdvisor}
 *       发起的澄清问题的回答（槽位补全）；</li>
 *   <li>{@link #approval} —— 对高风险工具调用的人工确认结果。</li>
 * </ul>
 *
 * <p>使用 {@link Builder} 模式构建实例，保证对象一旦创建即不可变（字段无 public setter）。</p>
 *
 * @see AgentEngine#chat(AgentRequest)
 * @see AgentResponse
 */
public class AgentRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String message;
    private Map<String, Object> slotAnswers;
    private ApprovalInfo approval;

    /**
     * 构建器入口。
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getSlotAnswers() {
        return slotAnswers;
    }

    public ApprovalInfo getApproval() {
        return approval;
    }

    /**
     * 高风险工具调用的人工审批信息。
     *
     * <p>当工具的 {@link io.github.aiagent.core.tool.annotation.AgentTool.RiskLevel}
     * 为 {@code HIGH} 时，引擎可能暂停执行并等待用户确认。
     * 用户确认后，前端将审批结果封装到此对象中随下一次请求发送。</p>
     */
    public static class ApprovalInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String approvalId;
        private boolean approved;
        private String comment;

        public String getApprovalId() {
            return approvalId;
        }

        public void setApprovalId(String approvalId) {
            this.approvalId = approvalId;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    /**
     * Builder 实现。
     */
    public static class Builder {
        private final AgentRequest target = new AgentRequest();

        public Builder sessionId(String sessionId) {
            target.sessionId = sessionId;
            return this;
        }

        public Builder message(String message) {
            target.message = message;
            return this;
        }

        public Builder slotAnswers(Map<String, Object> slotAnswers) {
            target.slotAnswers = slotAnswers;
            return this;
        }

        public Builder approval(ApprovalInfo approval) {
            target.approval = approval;
            return this;
        }

        public AgentRequest build() {
            return target;
        }
    }
}
