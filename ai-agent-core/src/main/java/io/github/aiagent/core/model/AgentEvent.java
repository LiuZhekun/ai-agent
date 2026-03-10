package io.github.aiagent.core.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * SSE 事件标准模型。
 */
public class AgentEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String sessionId;
    private AgentEventType type;
    private Instant timestamp;
    private Object payload;
    private String traceId;

    /**
     * 基于类型、会话和载荷快速创建事件。
     *
     * @param type      事件类型
     * @param sessionId 会话 ID
     * @param payload   事件载荷
     * @return 创建后的事件对象
     */
    public static AgentEvent of(AgentEventType type, String sessionId, Object payload) {
        AgentEvent event = new AgentEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setSessionId(sessionId);
        event.setType(type);
        event.setTimestamp(Instant.now());
        event.setPayload(payload);
        return event;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public AgentEventType getType() {
        return type;
    }

    public void setType(AgentEventType type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgentEvent that)) {
            return false;
        }
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
