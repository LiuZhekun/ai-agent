# ai-agent-knowledge

## 模块目的

提供分层知识增强能力，支持 L0/L1/L2/L3 四层按需注入。每一层均有独立的 `enabled` 开关，业务项目可精确控制启停。

## 能力分层

| 层级 | 能力 | 配置前缀 | 默认 |
|------|------|---------|------|
| L0 | 静态知识片段 | `ai.agent.knowledge.snippet` | 启用 |
| L1 | 数据库 Schema 自动发现 | `ai.agent.knowledge.schema` | 启用 |
| L2 | 安全 SQL 执行（校验/限流/脱敏/Explain） | `ai.agent.knowledge.sql` | 启用 |
| L3 | RAG 检索增强（切分/检索/重排/引用组装） | `ai.agent.knowledge.rag` | 启用 |

## 独立开关配置

每一层都可通过 `enabled` 属性独立控制，关闭后该层完全不参与知识组装或工具注册：

```yaml
ai:
  agent:
    knowledge:
      snippet:
        enabled: true          # L0 开关
        path: classpath*:agent-knowledge/*.*
      schema:
        enabled: true          # L1 开关（需 MySQL）
      sql:
        enabled: true          # L2 开关（需 MySQL）
        # ... 其他安全策略配置
      rag:
        enabled: true          # L3 开关（需 Milvus）
        chunk-size: 400
        chunk-overlap: 80
        top-k: 8
        rerank-top-k: 5
        min-score: 0.05
        max-context-chars: 2200
```

典型组合示例：

- **纯对话**（无数据库）：L0=true, L1=false, L2=false, L3=false
- **业务查询**（有 MySQL，无向量库）：L0=true, L1=true, L2=true, L3=false
- **全功能**：全部 true

## 运行机制

1. 启动后 `RagFullIndexer` 从 L0 片段构建全量索引（仅 L3 启用时生效）。
2. 用户请求到达时，`KnowledgeAdvisor` 将 query 传入 `KnowledgeManager`。
3. `KnowledgeManager` 根据各层 `enabled` 状态按需组装知识上下文注入 Prompt。
4. L2 安全 SQL 作为 Agent 工具独立运行，`enabled=false` 时工具调用返回禁用提示。

## 注意事项

- L1/L2 依赖 MySQL 数据源，未配置数据源时请关闭对应开关以避免启动异常。
- L3 依赖 Milvus 向量数据库，默认使用 `spring.ai.vectorstore.milvus` 连接配置。
- 各层开关仅控制运行时行为，对应的 Spring Bean 仍会注册（轻量级，不占额外资源）。
