package io.github.aiagent.core.agent;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 执行轨迹。
 */
public class ExecutionTrace implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String traceId;
    private String toolName;
    private String toolGroup;
    private Map<String, Object> input;
    private Object output;
    private TraceStatus status;
    private Instant startTime;
    private long durationMs;
    private String errorMessage;

    /**
     * 轨迹状态。
     */
    public enum TraceStatus {
        /** 已开始执行。 */
        STARTED,
        /** 执行成功。 */
        SUCCESS,
        /** 执行失败。 */
        FAILED
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolGroup() {
        return toolGroup;
    }

    public void setToolGroup(String toolGroup) {
        this.toolGroup = toolGroup;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public TraceStatus getStatus() {
        return status;
    }

    public void setStatus(TraceStatus status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
