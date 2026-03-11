package io.github.aiagent.starter.endpoint;

import io.github.aiagent.core.tool.AgentToolCallbackProvider;
import io.github.aiagent.core.tool.ToolMetadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具元数据查询接口 —— 供前端或调试端获取当前 Agent 可用的工具列表。
 * <p>
 * 返回内容包括工具名称、描述、参数 Schema 及风险等级等信息，
 * 可用于前端展示"Agent 能力概览"或开发期间的接口调试。
 * <p>
 * 端点：{@code GET /api/agent/tools}
 *
 * @see AgentToolCallbackProvider
 * @see ToolMetadata
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
