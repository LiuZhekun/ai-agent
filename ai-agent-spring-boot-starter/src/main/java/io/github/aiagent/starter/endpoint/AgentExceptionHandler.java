package io.github.aiagent.starter.endpoint;

import io.github.aiagent.core.exception.AgentException;
import io.github.aiagent.core.exception.SessionBusyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Agent 全局异常处理 —— 将框架内的异常统一转换为结构化的 JSON 错误响应。
 * <p>
 * 仅作用于 {@code io.github.aiagent.starter.endpoint} 包下的控制器。
 * 响应体格式统一为 {@code {"error": "<code>", "message": "<msg>", "recoverable": <bool>}}，
 * 前端可根据 {@code recoverable} 决定是否提示用户重试。
 * <p>
 * 当前处理的异常类型：
 * <ul>
 *   <li>{@link SessionBusyException} → HTTP 409（会话正在处理中，可重试）</li>
 *   <li>{@link AgentException} → HTTP 500（框架内部错误，根据 {@code recoverable} 判断）</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "io.github.aiagent.starter.endpoint")
public class AgentExceptionHandler {

    @ExceptionHandler(SessionBusyException.class)
    public ResponseEntity<Map<String, Object>> handleSessionBusy(SessionBusyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "SESSION_BUSY",
                        "message", ex.getMessage(),
                        "recoverable", true));
    }

    @ExceptionHandler(AgentException.class)
    public ResponseEntity<Map<String, Object>> handleAgentException(AgentException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", ex.getErrorCode(),
                        "message", ex.getMessage(),
                        "recoverable", ex.isRecoverable()));
    }
}
