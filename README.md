# AI Agent

一个可扩展的多模块 AI Agent 工程，支持会话编排、工具调用、知识增强（L0/L1/L2/L3 RAG）、向量化、字段翻译、SSE 前端交互与一键部署。

## 文档入口

- 项目文档目录：
  - [00-项目概述](docs/00-项目概述.md)
  - [01-架构设计](docs/01-架构设计.md)
  - [02-开发环境搭建](docs/02-开发环境搭建.md)
  - [03-中间件部署手册](docs/03-中间件部署手册.md)
  - [04-快速开始](docs/04-快速开始.md)
  - [05-工具开发指南](docs/05-工具开发指南.md)
  - [06-向量化接入指南](docs/06-向量化接入指南.md)
  - [07-字段翻译指南](docs/07-字段翻译指南.md)
  - [07-sse-e2e联调清单](docs/07-sse-e2e联调清单.md)
  - [08-前端开发指南](docs/08-前端开发指南.md)
  - [09-集成指南](docs/09-集成指南.md)
  - [10-API文档](docs/10-API文档.md)
  - [11-可观测与测试指南](docs/11-可观测与测试指南.md)
  - [12-问题答疑-L0L1L2L3与LLM执行顺序](docs/12-问题答疑FAQ)
- 部署文档：[deploy/README.md](deploy/README.md)
- 模块文档：各子模块 README（示例：[ai-agent-knowledge/README.md](ai-agent-knowledge/README.md)）

## 模块结构

```mermaid
flowchart LR
  core[ai-agent-core]
  knowledge[ai-agent-knowledge]
  vectorizer[ai-agent-vectorizer]
  translator[ai-agent-translator]
  starter[ai-agent-spring-boot-starter]
  demo[ai-agent-demo]
  ui[ai-agent-ui]
  deploy[deploy]

  core --> knowledge
  core --> vectorizer
  core --> translator
  core --> starter
  knowledge --> starter
  vectorizer --> starter
  translator --> starter
  starter --> demo
  demo --> ui
  deploy --> demo
```

## 业务生命周期（从用户输入一句话开始）

### 1) 在线对话总览流程图

```mermaid
flowchart TD
    A[用户输入一句话<br/>message + sessionId] --> B[前端调用 POST /api/agent/chat]
    B --> C[后端建立 SSE 流]
    C --> D[AgentEngine 获取会话与会话锁<br/>执行 Advisor 链]
    D --> E[KnowledgeAdvisor 组装 Prompt 上下文<br/>L0/L1/L2/L3 按开关注入]
    E --> F[LLM 意图理解与参数抽取]
    F --> G{参数是否完整}

    G -- 否 --> H[发送 CLARIFICATION_REQUIRED]
    H --> I[发送 COMPLETED<br/>等待用户补充 slotAnswers]
    I --> J[用户补充参数后再次调用 /chat]
    J --> D

    G -- 是 --> K{是否高风险工具}
    K -- 是 --> L[发送 EXECUTION_CONFIRM_REQUIRED]
    L --> M[发送 COMPLETED<br/>等待用户 approval]
    M --> N[用户确认后再次调用 /chat]
    N --> D

    K -- 否 --> O[执行工具或直接生成答案]
    O --> P[推送事件<br/>HEARTBEAT / THINKING_SUMMARY / TOOL_TRACE / CHART_PAYLOAD]
    P --> Q{执行是否出错}
    Q -- 是 --> R[发送 ERROR] --> S[发送 COMPLETED]
    Q -- 否 --> T[发送 FINAL_ANSWER] --> S
    S --> U[前端关闭本轮 SSE 连接]
```

### 2) 对话时序图（含澄清与审批）

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant FE as 前端 UI
    participant API as /api/agent/chat
    participant ENG as AgentEngine
    participant K as KnowledgeManager(L0/L1/L2/L3)
    participant LLM as LLM
    participant TOOL as AgentTool

    U->>FE: 输入一句话（message）
    FE->>API: POST chat(sessionId, message)
    API->>ENG: 获取会话 + 会话锁 + Advisor 链
    ENG->>K: 按开关注入知识上下文
    K-->>ENG: 返回 Prompt 上下文
    ENG->>LLM: 理解意图并抽取参数

    alt 参数不足（澄清分支）
        LLM-->>ENG: 缺少必填槽位
        ENG-->>FE: SSE: THINKING_SUMMARY
        ENG-->>FE: SSE: CLARIFICATION_REQUIRED
        ENG-->>FE: SSE: COMPLETED
        FE-->>U: 展示澄清问题
        U->>FE: 提交 slotAnswers
        FE->>API: POST chat(sessionId, slotAnswers)
        API->>ENG: 续接会话继续执行
    else 参数完整
        LLM-->>ENG: 可以执行
    end

    alt 高风险工具（审批分支）
        ENG-->>FE: SSE: EXECUTION_CONFIRM_REQUIRED
        ENG-->>FE: SSE: COMPLETED
        FE-->>U: 弹窗确认
        U->>FE: 提交 approval
        FE->>API: POST chat(sessionId, approval)
        API->>ENG: 续接会话继续执行
    else 低风险或无需工具
        Note over ENG: 直接执行工具或生成回答
    end

    ENG-->>FE: SSE: HEARTBEAT / THINKING_SUMMARY
    ENG->>TOOL: 调用工具
    TOOL-->>ENG: 返回结果或异常
    ENG-->>FE: SSE: TOOL_TRACE

    opt 需要图表展示
        ENG-->>FE: SSE: CHART_PAYLOAD
    end

    alt 成功
        ENG-->>FE: SSE: FINAL_ANSWER
        ENG-->>FE: SSE: COMPLETED
    else 失败
        ENG-->>FE: SSE: ERROR
        ENG-->>FE: SSE: COMPLETED
    end
```

### 3) 知识与向量支撑生命周期

```mermaid
flowchart LR
    subgraph Offline[离线/运维阶段]
      A1[L0 知识片段] --> A2[RagFullIndexer 构建索引]
      A3[业务数据源 MySQL] --> A4[L1 Schema 发现 + L2 Safe SQL 能力]
      A2 --> A5[Milvus 向量库]
      A6[手动触发 /api/agent/vector/sync] --> A7[增量向量同步]
      A7 --> A5
    end

    subgraph Online[在线请求阶段]
      B1[用户 query] --> B2[KnowledgeManager]
      B2 --> B3[L0/L1/L2/L3 按 enabled 组合上下文]
      B3 --> B4[注入 Prompt 后进入 Agent 主链路]
    end
```



## 核心能力

- 会话引擎：会话锁、记忆裁剪、澄清式对话、任务规划与执行追踪。
- 知识分层：L0 文本片段、L1 Schema 发现、L2 Safe SQL、L3 全量 RAG 引用上下文。
- 工具生态：`@AgentTool` 工具分组、装饰器拦截、超时/重试/并发限制。
- 部署体系：`deploy/install-all.sh` 支持 Linux 一键安装 Docker/Compose 并拉起服务。

## 环境要求

- Java 17+
- Maven 3.9+
- Node.js 18+
- Docker 24+（可由部署脚本自动安装）

## 快速开始

### 方式一：一键部署（Linux 推荐）

```bash
cd deploy
chmod +x install-all.sh
./install-all.sh
```

### 方式二：本地开发

```bash
# 1) 启动基础中间件
cd deploy
docker compose --env-file .env.example up -d mysql redis milvus

# 2) 启动后端
cd ../
mvn -pl ai-agent-demo -am spring-boot:run

# 3) 启动前端
cd ai-agent-ui
npm install
npm run dev
```

## 许可证

仅用于学习与内部验证