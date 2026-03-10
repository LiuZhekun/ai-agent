# ai-agent-knowledge

## 模块目的

提供分层知识增强能力，支持 L0/L1/L2/L3 四层注入。

## 能力分层

- `L0`：文本片段加载（`snippet`）。
- `L1`：数据库 Schema 自动发现（`schema`）。
- `L2`：安全 SQL 执行（`sql`，含校验/限流/脱敏/Explain）。
- `L3`：全量 RAG（`rag`，含切分、检索、重排、引用上下文组装、全量索引入口）。

## L3 RAG 配置项

```properties
ai.agent.knowledge.rag.enabled=true
ai.agent.knowledge.rag.chunk-size=400
ai.agent.knowledge.rag.chunk-overlap=80
ai.agent.knowledge.rag.top-k=8
ai.agent.knowledge.rag.rerank-top-k=5
ai.agent.knowledge.rag.min-score=0.05
ai.agent.knowledge.rag.max-context-chars=2200
```

## 运行机制

1. 启动后 `RagFullIndexer` 从 L0 片段构建全量索引。
2. 用户请求到达时，`KnowledgeAdvisor` 将 query 传入 `KnowledgeManager`。
3. `KnowledgeManager` 同时拼装 L0/L1/L2 信息，并注入带引用来源的 L3 RAG context。

## 注意事项

- L3 默认使用内存索引，适合通用能力和本地验证。
- 如需高精度检索，可在后续替换为向量库检索实现。
