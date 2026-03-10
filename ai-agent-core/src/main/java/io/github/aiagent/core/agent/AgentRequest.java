package io.github.aiagent.core.agent;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Agent 请求对象。
 * 承载用户消息、槽位补全答案和执行确认信息。
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
     * 执行确认信息。
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
