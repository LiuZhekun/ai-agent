package io.github.aiagent.core.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构化审计日志输出器。
 * <p>
 * 说明：这是审计与合规能力的基础组件，默认项目模板中未强制接入每条业务链路。
 * 当你需要追踪工具调用、安全拦截或 SQL 审计时，可在对应流程显式调用这里的方法。
 */
@Component
public class AgentAuditLogger {

    private static final Logger LOG = LoggerFactory.getLogger("agent-audit");
    private final ObjectMapper objectMapper;

    public AgentAuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一次工具调用审计事件。
     */
    public void logToolCall(String traceId, String sessionId, String userId, String toolName, Object input, Object output, long durationMs, boolean success) {
        Map<String, Object> event = base("tool_call", traceId, sessionId);
        event.put("userId", userId);
        event.put("toolName", toolName);
        event.put("input", input);
        event.put("output", output);
        event.put("durationMs", durationMs);
        event.put("success", success);
        write(event);
    }

    /**
     * 记录一次澄清回合审计事件。
     */
    public void logClarification(String traceId, String sessionId, List<String> missingSlots, int round) {
        Map<String, Object> event = base("clarification", traceId, sessionId);
        event.put("missingSlots", missingSlots);
        event.put("round", round);
        write(event);
    }

    /**
     * 记录一次 SQL 执行审计事件（包含是否放行与拒绝原因）。
     */
    public void logSqlExecution(String traceId, String sessionId, String sql, boolean allowed, String rejectReason) {
        Map<String, Object> event = base("sql_execution", traceId, sessionId);
        event.put("sql", sql);
        event.put("allowed", allowed);
        event.put("rejectReason", rejectReason);
        write(event);
    }

    private Map<String, Object> base(String eventName, String traceId, String sessionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("event", eventName);
        event.put("traceId", traceId);
        event.put("sessionId", sessionId);
        event.put("timestamp", Instant.now().toString());
        return event;
    }

    private void write(Map<String, Object> event) {
        try {
            LOG.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            LOG.warn("audit log serialize failed: {}", event, ex);
        }
    }
}
