package io.github.aiagent.core.tool;

/**
 * 工具调用拦截扩展点。
 */
public interface ToolCallbackInterceptor {

    void beforeCall(String toolName, String toolInput);

    String afterCall(String toolName, String toolInput, String toolOutput);
}
