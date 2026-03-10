package io.github.aiagent.core.agent;

import io.github.aiagent.core.model.AgentEvent;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 响应封装。
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
