package io.github.aiagent.core.agent.advisor;

import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.agent.ExecutionTrace;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 轨迹采集 Advisor。
 */
@Component
@Order(40)
public class TraceAdvisor {

    /**
     * 开始记录轨迹。
     */
    public ExecutionTrace startTrace(AgentSession session, String toolName, String toolGroup) {
        ExecutionTrace trace = new ExecutionTrace();
        trace.setTraceId(UUID.randomUUID().toString());
        trace.setToolName(toolName);
        trace.setToolGroup(toolGroup);
        trace.setStatus(ExecutionTrace.TraceStatus.STARTED);
        trace.setStartTime(Instant.now());
        session.getExecutionTraces().add(trace);
        return trace;
    }

    /**
     * 结束轨迹。
     */
    public void finishTrace(ExecutionTrace trace, boolean success, String errorMessage) {
        trace.setStatus(success ? ExecutionTrace.TraceStatus.SUCCESS : ExecutionTrace.TraceStatus.FAILED);
        long duration = Instant.now().toEpochMilli() - trace.getStartTime().toEpochMilli();
        trace.setDurationMs(Math.max(duration, 0));
        trace.setErrorMessage(errorMessage);
    }
}
