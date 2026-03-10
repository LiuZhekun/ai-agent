package io.github.aiagent.core.agent;

import io.github.aiagent.core.agent.advisor.MemoryAdvisor;
import io.github.aiagent.core.agent.advisor.ThinkingSummaryAdvisor;
import io.github.aiagent.core.exception.ClarificationRequiredException;
import io.github.aiagent.core.exception.SessionBusyException;
import io.github.aiagent.core.model.AgentEvent;
import io.github.aiagent.core.model.AgentEventType;
import io.github.aiagent.core.model.ChatMessage;
import io.github.aiagent.core.prompt.SystemPromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent 引擎入口。
 * 组装 ChatClient + Advisor 链 + ToolCallbackProvider，委托 Spring AI 执行工具调用循环。
 */
@Component
public class AgentEngine {

    private final ChatClient.Builder chatClientBuilder;
    private final AgentSessionManager sessionManager;
    private final List<AgentAdvisor> advisors;
    private final ToolCallbackProvider toolCallbackProvider;
    private final MemoryAdvisor memoryAdvisor;
    private final ThinkingSummaryAdvisor thinkingSummaryAdvisor;
    private final SystemPromptBuilder systemPromptBuilder;

    public AgentEngine(
            ChatClient.Builder chatClientBuilder,
            AgentSessionManager sessionManager,
            List<AgentAdvisor> advisors,
            ToolCallbackProvider toolCallbackProvider,
            MemoryAdvisor memoryAdvisor,
            ThinkingSummaryAdvisor thinkingSummaryAdvisor,
            SystemPromptBuilder systemPromptBuilder) {
        this.chatClientBuilder = chatClientBuilder;
        this.sessionManager = sessionManager;
        this.advisors = advisors;
        this.toolCallbackProvider = toolCallbackProvider;
        this.memoryAdvisor = memoryAdvisor;
        this.thinkingSummaryAdvisor = thinkingSummaryAdvisor;
        this.systemPromptBuilder = systemPromptBuilder;
    }

    /**
     * 处理对话并返回标准 SSE 事件流（含心跳）。
     */
    public Flux<AgentEvent> chat(AgentRequest request) {
        String sid = request.getSessionId();

        Flux<AgentEvent> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(i -> AgentEvent.of(AgentEventType.HEARTBEAT, sid, "ping"));

        Flux<AgentEvent> main = Flux.<AgentEvent>create(sink -> {
            AgentSession session = sessionManager.getOrCreateSession(sid);
            try {
                session.tryLock(5, TimeUnit.SECONDS);
            } catch (SessionBusyException ex) {
                sink.error(ex);
                return;
            }
            try {
                session.touch();

                List<ChatMessage> history = memoryAdvisor.before(session);
                session.getMetadata().put("conversationHistory",
                        SystemPromptBuilder.formatHistory(history));

                for (AgentAdvisor advisor : advisors) {
                    advisor.before(session, request);
                }

                ChatClient chatClient = chatClientBuilder.build();
                String message = request.getMessage() == null ? "" : request.getMessage();

                String response = chatClient.prompt()
                        .system(systemPromptBuilder.build(session))
                        .user(message)
                        .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                        .call()
                        .content();

                memoryAdvisor.after(request);
                memoryAdvisor.saveAssistant(sid, response);

                for (AgentAdvisor advisor : advisors) {
                    advisor.after(session, response);
                }

                sink.next(AgentEvent.of(AgentEventType.THINKING_SUMMARY, sid,
                        thinkingSummaryAdvisor.summarize(response)));
                sink.next(AgentEvent.of(AgentEventType.FINAL_ANSWER, sid, response));
                sink.next(AgentEvent.of(AgentEventType.COMPLETED, sid, "completed"));
                sink.complete();
            } catch (ClarificationRequiredException ex) {
                sink.next(AgentEvent.of(AgentEventType.CLARIFICATION_REQUIRED, sid,
                        Map.of("clarificationId", ex.getClarificationId(),
                                "missingSlots", ex.getMissingSlots())));
                sink.next(AgentEvent.of(AgentEventType.COMPLETED, sid, "clarification_needed"));
                sink.complete();
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                sink.next(AgentEvent.of(AgentEventType.ERROR, sid,
                        Map.of("message", msg, "recoverable", false)));
                sink.next(AgentEvent.of(AgentEventType.COMPLETED, sid, "error"));
                sink.complete();
            } finally {
                session.unlock();
            }
        }).subscribeOn(Schedulers.boundedElastic());

        return Flux.merge(heartbeat, main)
                .takeUntil(e -> e.getType() == AgentEventType.COMPLETED);
    }

    public List<AgentAdvisor> getAdvisors() {
        return advisors;
    }

    public ToolCallbackProvider getToolCallbackProvider() {
        return toolCallbackProvider;
    }
}
