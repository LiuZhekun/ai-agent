# ai-agent-core

## 模块目的

提供 Agent 核心运行时：会话管理、Advisor 链、工具执行装饰、记忆系统、规划/澄清、响应格式化。

## 主要包

- `core.agent`：`AgentEngine`、`AgentSession`、请求与事件模型。
- `core.agent.advisor`：记忆、澄清、规划、轨迹、思考摘要等 Advisor。
- `core.tool`：工具元数据、拦截器、回调装饰器、Provider。
- `core.memory`：会话记忆、Token 裁剪、摘要压缩。

## 扩展点

- `ToolCallbackInterceptor`：工具调用前后拦截。
- `VectorKnowledgeBase`：长期记忆/向量检索接口。
- 自定义 Advisor：通过 Spring Bean 注入并参与引擎调用。

## 使用示例

```java
AgentRequest request = AgentRequest.builder()
        .sessionId("s-001")
        .message("查询最近一周新增用户")
        .build();
Flux<AgentEvent> stream = agentEngine.chat(request);
```

## 注意事项

- 同一 `sessionId` 默认并发互斥，避免状态污染。
- 建议生产环境启用 Redis 记忆持久化。
- 工具回调必须设置超时和重试参数，避免链路阻塞。
