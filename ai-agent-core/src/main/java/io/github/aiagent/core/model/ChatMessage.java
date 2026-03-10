package io.github.aiagent.core.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 对话消息模型。
 */
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Role role;
    private String content;
    private Instant timestamp;
    private Map<String, Object> metadata;

    /**
     * 消息角色。
     */
    public enum Role {
        /** 用户消息。 */
        USER,
        /** 助手消息。 */
        ASSISTANT,
        /** 系统消息。 */
        SYSTEM
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
