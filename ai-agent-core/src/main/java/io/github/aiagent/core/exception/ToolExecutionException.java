package io.github.aiagent.core.exception;

import java.io.Serial;

/**
 * 工具执行失败异常。
 * 常见触发条件：调用超时、参数错误、并发限流触发、目标服务不可用。
 */
public class ToolExecutionException extends AgentException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String toolName;
    private final Object toolInput;

    /**
     * 构造工具执行异常。
     *
     * @param message  异常描述
     * @param toolName 工具名
     * @param toolInput 工具输入
     */
    public ToolExecutionException(String message, String toolName, Object toolInput) {
        super("TOOL_EXECUTION_ERROR", message, false);
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    public ToolExecutionException(String message, String toolName, Object toolInput, Throwable cause) {
        super("TOOL_EXECUTION_ERROR", message, cause, false);
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    public String getToolName() {
        return toolName;
    }

    public Object getToolInput() {
        return toolInput;
    }
}
