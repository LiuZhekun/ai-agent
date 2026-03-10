package io.github.aiagent.core.exception;

import java.io.Serial;

/**
 * Agent 运行时异常基类。
 * 用于承载统一错误码与可恢复性标识，便于前端与网关做一致化处理。
 */
public class AgentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final boolean recoverable;

    /**
     * 构造基础异常。
     *
     * @param errorCode   错误码
     * @param message     异常描述
     * @param recoverable 是否可恢复
     */
    public AgentException(String errorCode, String message, boolean recoverable) {
        super(message);
        this.errorCode = errorCode;
        this.recoverable = recoverable;
    }

    /**
     * 构造基础异常。
     *
     * @param errorCode   错误码
     * @param message     异常描述
     * @param cause       原始异常
     * @param recoverable 是否可恢复
     */
    public AgentException(String errorCode, String message, Throwable cause, boolean recoverable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.recoverable = recoverable;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRecoverable() {
        return recoverable;
    }
}
