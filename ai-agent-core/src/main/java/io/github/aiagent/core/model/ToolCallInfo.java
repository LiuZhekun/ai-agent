package io.github.aiagent.core.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 工具调用过程信息。
 */
public class ToolCallInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String toolName;
    private String toolGroup;
    private Map<String, Object> parameters;
    private Object result;
    private ToolCallStatus status;
    private long durationMs;
    private String errorMessage;

    /**
     * 工具调用状态。
     */
    public enum ToolCallStatus {
        /** 已开始调用。 */
        STARTED,
        /** 调用成功。 */
        SUCCESS,
        /** 调用失败。 */
        FAILED
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

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public ToolCallStatus getStatus() {
        return status;
    }

    public void setStatus(ToolCallStatus status) {
        this.status = status;
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
