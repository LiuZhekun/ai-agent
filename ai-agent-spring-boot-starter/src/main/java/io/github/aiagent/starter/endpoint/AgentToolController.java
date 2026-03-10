package io.github.aiagent.starter.endpoint;

import io.github.aiagent.core.tool.AgentToolCallbackProvider;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具元数据查询接口。
 * 给前端或调试端展示当前可用工具列表、参数信息与风险等级。
 */
@RestController
@RequestMapping("/api/agent/tools")
public class AgentToolController {

    private final AgentToolCallbackProvider provider;

    public AgentToolController(AgentToolCallbackProvider provider) {
        this.provider = provider;
    }

    /**
     * 返回启动时自动发现的工具元数据快照。
     */
    @GetMapping
    public List<ToolMetadata> listTools() {
        return provider.getToolMetadatas();
    }
}
