package io.github.aiagent.core.metrics;

/**
 * 指标常量定义。
 */
public final class MetricsConstants {

    private MetricsConstants() {
    }

    public static final String AGENT_TOKEN_CONSUMED = "agent.token.consumed";
    public static final String AGENT_TOOL_CALLS = "agent.tool.calls";
    public static final String AGENT_TOOL_SUCCESS_RATE = "agent.tool.success.rate";
    public static final String AGENT_TOOL_DURATION = "agent.tool.duration";
    public static final String AGENT_SQL_REJECTED = "agent.sql.rejected";
    public static final String AGENT_SESSION_DURATION = "agent.session.duration";
    public static final String AGENT_SESSION_ACTIVE = "agent.session.active";

    public static final String TAG_TOOL_NAME = "tool.name";
    public static final String TAG_TOOL_GROUP = "tool.group";
    public static final String TAG_SESSION_ID = "session.id";
    public static final String TAG_ERROR_TYPE = "error.type";
}
