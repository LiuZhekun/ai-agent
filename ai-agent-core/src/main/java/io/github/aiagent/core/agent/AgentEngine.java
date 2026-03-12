package io.github.aiagent.core.agent;

import io.github.aiagent.core.agent.advisor.MemoryAdvisor;
import io.github.aiagent.core.agent.advisor.PlanningAdvisor;
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
 * Agent 引擎 —— 整个对话流程的核心编排器。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>将 Spring AI {@link ChatClient}、{@link AgentAdvisor} 责任链和
 *       {@link ToolCallbackProvider} 组装成一条完整的对话处理管线；</li>
 *   <li>以 SSE {@link reactor.core.publisher.Flux} 事件流的形式向调用方推送心跳、
 *       思考摘要、最终回答及错误等事件；</li>
 *   <li>通过 {@link AgentSession} 的 {@link java.util.concurrent.locks.ReentrantLock}
 *       保证同一会话不会被并发请求污染。</li>
 * </ul>
 *
 * <h3>调用链路</h3>
 * <pre>
 *   chat() → 获取/创建 Session → 加锁
 *         → MemoryAdvisor.before()（加载历史）
 *         → 各 AgentAdvisor.before()（澄清/规划等）
 *         → ChatClient.call()（含工具调用循环）
 *         → MemoryAdvisor.after() + saveAssistant()
 *         → 各 AgentAdvisor.after()
 *         → 发送 SSE 事件 → 释放锁
 * </pre>
 *
 * <h3>设计决策</h3>
 * <p>选择同步 {@code call()} 而非流式 {@code stream()} 是因为当前主链路需要拿到完整回复
 * 后才能执行 after-advisor 逻辑和摘要生成；外层通过 {@code Flux.create()} +
 * {@code Schedulers.boundedElastic()} 保证不阻塞 Reactor 事件循环。</p>
 *
 * @see AgentAdvisor
 * @see AgentSession
 * @see AgentSessionManager
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
    private final PlanningAdvisor planningAdvisor;

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
        this.planningAdvisor = advisors.stream()
                .filter(PlanningAdvisor.class::isInstance)
                .map(PlanningAdvisor.class::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * 处理一次用户对话请求，返回包含心跳的 SSE 事件流。
     *
     * <p>事件流由两路 {@link Flux} 合并而成：</p>
     * <ul>
     *   <li><b>heartbeat</b> —— 每 15 秒发送 {@code HEARTBEAT} 事件，防止反向代理/浏览器超时断开连接；</li>
     *   <li><b>main</b> —— 在 {@code boundedElastic} 线程池上执行完整的 Advisor → LLM → Advisor 链路。</li>
     * </ul>
     * <p>流以 {@code COMPLETED} 事件终止（无论成功、需澄清还是出错）。</p>
     *
     * @param request 包含 sessionId、用户消息及可选槽位补全的请求对象
     * @return 事件流，调用方应以 SSE 方式消费
     * @throws io.github.aiagent.core.exception.SessionBusyException 若同一会话在 5 秒内无法获取锁
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

                if (planningAdvisor != null) {
                    for (AgentEvent event : planningAdvisor.executeIfPresent(session).toIterable()) {
                        sink.next(event);
                    }
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

    /**
     * 返回当前引擎加载的所有 {@link AgentAdvisor}（按 {@code @Order} 排序）。
     * 主要供测试和管理端点使用。
     *
     * @return 不可变排序后的 Advisor 列表
     */
    public List<AgentAdvisor> getAdvisors() {
        return advisors;
    }

    /**
     * 返回引擎使用的 {@link ToolCallbackProvider}，其内部已对所有工具回调
     * 应用了限流、超时、重试和拦截器装饰。
     *
     * @return 工具回调提供者
     */
    public ToolCallbackProvider getToolCallbackProvider() {
        return toolCallbackProvider;
    }
}
