package io.github.aiagent.core.agent;

import io.github.aiagent.core.model.AgentEvent;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 响应封装 —— 聚合一次对话请求产生的所有输出。
 *
 * <p>该对象主要用于非 SSE 的同步场景（如测试或 REST 回调），将流式事件归集为单个结构：</p>
 * <ul>
 *   <li>{@link #events} —— 完整的事件列表（与 SSE 流中的事件一一对应）；</li>
 *   <li>{@link #thinkingSummary} —— LLM 思考过程的面向用户摘要；</li>
 *   <li>{@link #traces} —— 本次请求涉及的所有工具调用执行轨迹。</li>
 * </ul>
 *
 * @see AgentRequest
 * @see ExecutionTrace
 * @see ThinkingSummary
 */
public class AgentResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private List<AgentEvent> events = new ArrayList<>();
    private ThinkingSummary thinkingSummary;
    private List<ExecutionTrace> traces = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<AgentEvent> getEvents() {
        return events;
    }

    public void setEvents(List<AgentEvent> events) {
        this.events = events;
    }

    public ThinkingSummary getThinkingSummary() {
        return thinkingSummary;
    }

    public void setThinkingSummary(ThinkingSummary thinkingSummary) {
        this.thinkingSummary = thinkingSummary;
    }

    public List<ExecutionTrace> getTraces() {
        return traces;
    }

    public void setTraces(List<ExecutionTrace> traces) {
        this.traces = traces;
    }
}
