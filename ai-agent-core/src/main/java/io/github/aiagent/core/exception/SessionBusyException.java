package io.github.aiagent.core.exception;

import java.io.Serial;

/**
 * 会话忙异常。
 * 触发条件：同一会话在短时间内被并发请求，无法获取会话锁。
 * 恢复建议：客户端稍后重试或切换新会话。
 */
public class SessionBusyException extends AgentException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String sessionId;

    /**
     * 构造会话忙异常。
     *
     * @param sessionId 会话 ID
     */
    public SessionBusyException(String sessionId) {
        super("SESSION_BUSY", "Session is busy: " + sessionId, true);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
