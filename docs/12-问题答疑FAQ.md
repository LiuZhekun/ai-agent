# 问题答疑：

## 目的

沉淀关于知识分层与 LLM 调用顺序的高频问题，统一团队认知，避免在联调和排障时出现理解偏差。

## 问题 1：如果 L0/L1/L2/L3 全都开启，当前代码中的执行顺序是什么？

### 简答

不是简单的 `L0 -> L1 -> L2 -> L3 -> LLM`。  
当前实现是：

1. 启动阶段先准备 L3 索引（基于 L0 片段切分）。
2. 每次请求先在 Advisor 链中组装 `L0 + L1 + L3` 到 Prompt 上下文。
3. 然后进入 LLM 主调用。
4. L2 作为工具，在 LLM 调用过程中按需触发执行（不是预先拼到 Prompt 里）。

### 代码级顺序（一次 chat 请求）

1. `AgentEngine.chat()` 获取会话并加锁。
2. `MemoryAdvisor.before()` 加载历史对话。
3. 按 `@Order` 执行 Advisor：
   - `KnowledgeAdvisor`（10）
   - `ClarificationAdvisor`（20）
   - `PlanningAdvisor`（30）
4. `KnowledgeAdvisor.before()` 调用 `KnowledgeManager.getKnowledgePrompt(query)`。
5. `KnowledgeManager` 内部组装顺序：
   - L0：`KnowledgeSnippetLoader` 加载片段
   - L1：`SchemaDiscoveryService + SchemaPromptGenerator`
   - L3：`RagRetriever -> RagReranker -> RagContextAssembler`
6. `SystemPromptBuilder.build(session)` 注入 `{knowledge_snippets}` 后，进入 `ChatClient.prompt().call()`。
7. LLM 运行中可通过 `toolCallbacks` 调用工具；若调用 `SafeSqlQueryTool`，即进入 L2 执行路径。
8. 请求结束后写回记忆并推送事件（`THINKING_SUMMARY`、`FINAL_ANSWER`、`COMPLETED`）。

### 代码级时序图

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant E as AgentEngine
    participant M as MemoryAdvisor
    participant KA as KnowledgeAdvisor
    participant KM as KnowledgeManager
    participant SP as SystemPromptBuilder
    participant LLM as ChatClient/LLM
    participant TCP as AgentToolCallbackProvider
    participant TD as ToolCallbackDecorator
    participant L2 as SafeSqlQueryTool(L2)

    U->>E: chat(sessionId, message)
    E->>M: before(session)
    M-->>E: conversationHistory

    E->>KA: before(session, request)
    KA->>KM: getKnowledgePrompt(query)
    KM->>KM: L0 snippet
    KM->>KM: L1 schema
    KM->>KM: L3 retrieve/rerank/assemble
    KM-->>KA: knowledgeSnippets
    KA-->>E: session.metadata["knowledgeSnippets"]

    E->>SP: build(session)
    SP-->>E: system prompt
    E->>TCP: getToolCallbacks()
    TCP-->>E: toolCallbacks
    E->>LLM: prompt().call(toolCallbacks)

    opt LLM 需要实时业务数据
        LLM->>TD: invoke(safe-sql, args)
        TD->>L2: executeSql(sql)
        L2-->>TD: masked rows / error
        TD-->>LLM: formatted tool result
    end

    LLM-->>E: final content
    E-->>U: THINKING_SUMMARY / FINAL_ANSWER / COMPLETED
```

## 问题 2：LLM 在项目中起到了什么作用？

LLM 在当前项目中承担三类职责：

1. 语义理解与回答生成：理解用户问题并生成最终自然语言回答。
2. 工具决策与参数组织：在具备工具描述和上下文后，决定是否调用工具并组织入参。
3. 任务规划（可选链路）：`TaskPlanner` 中由 LLM 输出结构化 `TaskPlan`。

## 问题 3：为什么把 LLM 放到知识查询后面？

核心原因是 RAG/知识增强的一般范式：先准备证据，再让模型生成。

- 先注入 L0/L1/L3，可以给 LLM 明确事实依据，降低幻觉。
- 让模型基于业务上下文（术语、Schema、检索片段）做推理，而非凭参数记忆。
- 便于控制上下文边界和回答一致性。

> 注意：当前代码里“放在知识查询后面”主要指 L0/L1/L3。  
> L2 属于运行时工具能力，发生在 LLM 调用过程中，由模型按需触发。

## 常见误区

- 误区 1：`L2` 也会和 L0/L1/L3 一样在 `KnowledgeManager` 中预先注入。  
  实际：不会，L2 是工具调用路径。
- 误区 2：全开后一定每次都走 L2。  
  实际：只有模型判断需要实时查询时才会调用。
- 误区 3：LLM 只负责“润色文案”。  
  实际：LLM 还负责工具选择、参数组织和（可选）任务规划。

## 参考代码位置

- `ai-agent-core/src/main/java/io/github/aiagent/core/agent/AgentEngine.java`
- `ai-agent-knowledge/src/main/java/io/github/aiagent/knowledge/KnowledgeAdvisor.java`
- `ai-agent-knowledge/src/main/java/io/github/aiagent/knowledge/KnowledgeManager.java`
- `ai-agent-knowledge/src/main/java/io/github/aiagent/knowledge/sql/SafeSqlQueryTool.java`
- `ai-agent-core/src/main/java/io/github/aiagent/core/prompt/SystemPromptBuilder.java`
- `ai-agent-knowledge/src/main/java/io/github/aiagent/knowledge/rag/RagFullIndexer.java`
