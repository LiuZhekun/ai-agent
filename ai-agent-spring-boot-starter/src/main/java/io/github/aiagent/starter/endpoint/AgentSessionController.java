package io.github.aiagent.starter.endpoint;

import io.github.aiagent.core.agent.AgentSessionManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 会话管理接口。
 */
@RestController
@RequestMapping("/api/agent/sessions")
public class AgentSessionController {

    private final AgentSessionManager sessionManager;

    public AgentSessionController(AgentSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @GetMapping
    public Map<String, Object> activeSessions() {
        return Map.of("count", sessionManager.activeSessionCount());
    }

    @DeleteMapping("/{id}")
    public void closeSession(@PathVariable("id") String sessionId) {
        sessionManager.removeSession(sessionId);
    }
}
