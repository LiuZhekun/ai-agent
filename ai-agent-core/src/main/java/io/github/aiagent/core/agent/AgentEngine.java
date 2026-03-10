package io.github.aiagent.core.agent;

import io.github.aiagent.core.agent.advisor.MemoryAdvisor;
import io.github.aiagent.core.agent.advisor.ThinkingSummaryAdvisor;
import io.github.aiagent.core.model.AgentEvent;
import io.github.aiagent.core.model.AgentEventType;
import io.github.aiagent.core.prompt.SystemPromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * Agent 引擎入口。
 * 当前版本先提供稳定的会话并发控制和标准事件流输出。
 */
@Component
public class AgentEngine {

    private final ChatClient.Builder chatClientBuilder;
    private final AgentSessionManager sessionManager;
    private final List<Object> advisors;
    private final ToolCallbackProvider toolCallbackProvider;
    private final MemoryAdvisor memoryAdvisor;
    private final ThinkingSummaryAdvisor thinkingSummaryAdvisor;
    private final SystemPromptBuilder systemPromptBuilder;

    public AgentEngine(
            ChatClient.Builder chatClientBuilder,
            AgentSessionManager sessionManager,
            List<Object> advisors,
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
     * 处理对话并返回标准 SSE 事件流。
     *
     * @param request 请求参数
     * @return 事件流
     */
    public Flux<AgentEvent> chat(AgentRequest request) {
        AgentSession session = sessionManager.getOrCreateSession(request.getSessionId());
        session.tryLock(5, TimeUnit.SECONDS);
        try {
            session.touch();
            memoryAdvisor.before(session);
            invokeExtensionBefore(session, request);
            ChatClient chatClient = chatClientBuilder.build();
            String message = request.getMessage() == null ? "" : request.getMessage();
            String response = chatClient.prompt()
                    .system(systemPromptBuilder.build(session))
                    .user(message)
                    .call()
                    .content();
            memoryAdvisor.after(request);

            AgentEvent thinkingEvent = AgentEvent.of(
                    AgentEventType.THINKING_SUMMARY,
                    request.getSessionId(),
                    thinkingSummaryAdvisor.summarize(response));
            AgentEvent finalEvent = AgentEvent.of(
                    AgentEventType.FINAL_ANSWER,
                    request.getSessionId(),
                    response);
            AgentEvent completedEvent = AgentEvent.of(
                    AgentEventType.COMPLETED,
                    request.getSessionId(),
                    "completed");

            List<AgentEvent> events = new ArrayList<>();
            Stream.of(thinkingEvent, finalEvent, completedEvent).forEach(events::add);
            return Flux.fromIterable(events).delayElements(Duration.ofMillis(50));
        } finally {
            session.unlock();
        }
    }

    /**
     * 对外暴露当前加载的 advisor 列表。
     * 主要用于调试/测试场景核对扩展是否生效，主对话链路目前不直接调用该 getter。
     */
    public List<Object> getAdvisors() {
        return advisors;
    }

    /**
     * 对外暴露工具回调提供器。
     * 主要用于集成测试或外部编排接入；当前 chat 主路径不直接使用该 getter。
     */
    public ToolCallbackProvider getToolCallbackProvider() {
        return toolCallbackProvider;
    }

    /**
     * 通过轻量反射调用外部模块的 before(session, request/query) 扩展，
     * 使 core 不直接依赖可选模块，保持 L0/L1/L2 兼容。
     */
    private void invokeExtensionBefore(AgentSession session, AgentRequest request) {
        for (Object advisor : advisors) {
            if (advisor == memoryAdvisor || advisor == thinkingSummaryAdvisor || advisor == null) {
                continue;
            }
            Method[] methods = advisor.getClass().getMethods();
            for (Method method : methods) {
                if (!"before".equals(method.getName())) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                try {
                    if (types.length == 2 && types[0] == AgentSession.class && types[1] == AgentRequest.class) {
                        method.invoke(advisor, session, request);
                        break;
                    }
                    if (types.length == 2 && types[0] == AgentSession.class && types[1] == String.class) {
                        method.invoke(advisor, session, request.getMessage());
                        break;
                    }
                } catch (Exception ignored) {
                    // 可选模块失败不影响主流程，避免破坏原有 L0/L1/L2 能力。
                }
            }
        }
    }
}
