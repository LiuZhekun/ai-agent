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
 * 对话接口。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    private final AgentEngine agentEngine;

    public AgentChatController(AgentEngine agentEngine) {
        this.agentEngine = agentEngine;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentEvent> chat(@RequestBody AgentRequest request) {
        return agentEngine.chat(request);
    }
}
