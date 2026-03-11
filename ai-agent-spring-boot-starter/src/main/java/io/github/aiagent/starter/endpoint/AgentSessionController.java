package io.github.aiagent.starter.endpoint;

import io.github.aiagent.core.agent.AgentSessionManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 会话管理接口 —— 查询活跃会话数与手动关闭会话。
 * <p>
 * 端点：
 * <ul>
 *   <li>{@code GET /api/agent/sessions} — 获取当前活跃会话数量</li>
 *   <li>{@code DELETE /api/agent/sessions/{id}} — 关闭指定会话并释放资源</li>
 * </ul>
 *
 * @see AgentSessionManager
 */
@RestController
@RequestMapping("/api/agent/sessions")
public class AgentSessionController {

    private final AgentSessionManager sessionManager;

    public AgentSessionController(AgentSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 查询当前活跃会话数量。
     *
     * @return 包含 {@code count} 字段的 JSON
     */
    @GetMapping
    public Map<String, Object> activeSessions() {
        return Map.of("count", sessionManager.activeSessionCount());
    }

    /**
     * 关闭指定会话并释放关联的记忆与资源。
     *
     * @param sessionId 会话 ID
     */
    @DeleteMapping("/{id}")
    public void closeSession(@PathVariable("id") String sessionId) {
        sessionManager.removeSession(sessionId);
    }
}
