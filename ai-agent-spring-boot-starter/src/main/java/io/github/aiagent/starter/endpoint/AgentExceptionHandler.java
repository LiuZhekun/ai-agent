package io.github.aiagent.starter.endpoint;

import io.github.aiagent.core.exception.AgentException;
import io.github.aiagent.core.exception.SessionBusyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Agent 全局异常处理。
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
