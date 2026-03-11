package io.github.aiagent.starter.endpoint;

import io.github.aiagent.core.agent.AgentEngine;
import io.github.aiagent.core.agent.AgentRequest;
import io.github.aiagent.core.model.AgentEvent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Agent 对话接口 —— 基于 SSE（Server-Sent Events）协议的流式对话端点。
 * <p>
 * 客户端通过 {@code POST /api/agent/chat} 发送 {@link AgentRequest}，
 * 服务端以 {@code text/event-stream} 格式持续推送 {@link AgentEvent}，
 * 包括文本片段、工具调用、追问提示、心跳等事件类型。
 * <p>
 * 前端应监听 SSE 事件流并根据 {@link AgentEvent} 的 {@code type} 字段分发处理。
 * 连接断开后可根据 {@code sessionId} 恢复上下文。
 *
 * @see AgentEngine
 * @see AgentEvent
 */
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    private final AgentEngine agentEngine;

    public AgentChatController(AgentEngine agentEngine) {
        this.agentEngine = agentEngine;
    }

    /**
     * 发起流式对话。
     * <p>
     * 响应类型为 {@code text/event-stream}，每个 SSE 事件对应一个 {@link AgentEvent}。
     * 对话过程中可能穿插工具调用、追问、心跳等多种事件。
     *
     * @param request 对话请求，包含用户消息、sessionId 等
     * @return 持续推送的事件流
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentEvent> chat(@RequestBody AgentRequest request) {
        return agentEngine.chat(request);
    }
}
