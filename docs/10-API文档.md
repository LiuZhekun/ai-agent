# API 文档

## 1. 概述

AI-Agent 后端通过 REST + SSE（Server-Sent Events）混合协议对外提供服务。所有端点由 `ai-agent-spring-boot-starter` 自动注册，引入 Starter 依赖后即可使用，无需手动声明 Controller。

### 1.1 基础 URL

```
http://{host}:{port}/api/agent
```

默认本地开发地址为 `http://127.0.0.1:8080/api/agent`。

### 1.2 通用约定

| 约定 | 说明 |
|------|------|
| 请求格式 | `Content-Type: application/json; charset=UTF-8` |
| 响应格式 | 普通接口返回 `application/json`；对话接口返回 `text/event-stream`（SSE） |
| 字符编码 | 统一 UTF-8 |
| 时间格式 | ISO-8601（如 `2026-03-11T08:30:00Z`），时区为 UTC |
| 唯一标识 | `sessionId`、`eventId`、`approvalId` 均为 UUID v4 字符串 |
| 幂等性 | SSE 事件通过 `eventId` 保证幂等，客户端应基于 `eventId` 去重 |

### 1.3 接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/agent/chat` | 发起流式对话 |
| `GET` | `/api/agent/tools` | 查询可用工具列表 |
| `GET` | `/api/agent/sessions` | 查询活跃会话数量 |
| `DELETE` | `/api/agent/sessions/{id}` | 关闭指定会话 |
| `POST` | `/api/agent/vector/sync` | 手动触发向量同步 |
| `GET` | `/api/agent/vector/status` | 查询向量同步状态 |

---

## 2. 对话接口

### `POST /api/agent/chat`

核心对话端点。客户端发送 JSON 请求，服务端以 SSE 流持续推送事件，直到会话结束（收到 `COMPLETED` 事件）。

### 2.1 请求体结构

```json
{
  "sessionId": "string",
  "message": "string",
  "slotAnswers": { },
  "approval": { }
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | `string` | 是 | 会话唯一标识（UUID v4）。相同 `sessionId` 共享对话上下文与记忆。首次对话由客户端生成，后续请求复用同一值即可续接会话。 |
| `message` | `string` | 条件必填 | 用户输入的自然语言消息。在普通对话场景下必填；当请求仅携带 `slotAnswers` 或 `approval` 时可留空。 |
| `slotAnswers` | `Map<string, any>` | 否 | 对澄清问题的回答。键为槽位名称（与 `CLARIFICATION_REQUIRED` 事件中 `questions[].slotName` 对应），值为用户填写的答案。仅在补全澄清信息时携带。 |
| `approval` | `ApprovalInfo` | 否 | 高风险工具调用的人工审批信息。仅在回复 `EXECUTION_CONFIRM_REQUIRED` 事件时携带。 |

**`ApprovalInfo` 结构：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `approvalId` | `string` | 是 | 审批令牌，对应 `EXECUTION_CONFIRM_REQUIRED` 事件中的 `approvalToken`。 |
| `approved` | `boolean` | 是 | `true` 表示批准执行，`false` 表示拒绝。 |
| `comment` | `string` | 否 | 审批备注，可用于记录审批理由。 |

### 2.2 响应格式（SSE 流）

响应的 `Content-Type` 为 `text/event-stream`。每个 SSE 事件的 `data` 字段为一个 JSON 序列化的 `AgentEvent` 对象（详见 [第 6 节](#6-sse-事件协议详解)）。

```
data:{"eventId":"a1b2c3","sessionId":"s-1001","type":"THINKING_SUMMARY","timestamp":"2026-03-11T08:30:00Z","payload":"正在分析您的请求...","traceId":"trace-001"}

data:{"eventId":"d4e5f6","sessionId":"s-1001","type":"FINAL_ANSWER","timestamp":"2026-03-11T08:30:02Z","payload":"查询结果如下：...","traceId":"trace-001"}

data:{"eventId":"g7h8i9","sessionId":"s-1001","type":"COMPLETED","timestamp":"2026-03-11T08:30:02Z","payload":null,"traceId":"trace-001"}
```

> **注意**：每个 `data:` 行之间以空行分隔，符合 SSE 标准协议。

### 2.3 状态码

| HTTP 状态码 | 含义 | 说明 |
|-------------|------|------|
| `200` | 成功 | SSE 流正常建立，后续事件通过流推送。 |
| `400` | 请求参数错误 | `sessionId` 缺失或请求体格式不合法。 |
| `409` | 会话冲突 | 同一 `sessionId` 的上一次对话尚未结束（`SessionBusyException`）。客户端应等待上一次对话完成后重试，或使用新的 `sessionId`。 |
| `500` | 服务器内部错误 | Agent 引擎执行异常（`AgentException`）。根据响应中的 `recoverable` 字段判断是否可重试。 |

### 2.4 curl 示例

**普通对话：**

```bash
curl -N -X POST http://127.0.0.1:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "查询技术部所有用户"
  }'
```

**携带槽位回答（澄清补全）：**

```bash
curl -N -X POST http://127.0.0.1:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "slotAnswers": {
      "department": "技术部",
      "status": "active"
    }
  }'
```

**携带审批信息（确认执行）：**

```bash
curl -N -X POST http://127.0.0.1:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "approval": {
      "approvalId": "appr-7890-abcd",
      "approved": true,
      "comment": "确认删除"
    }
  }'
```

> `-N` 参数关闭 curl 的输出缓冲，以实时查看 SSE 事件。

---

## 3. 工具查询接口

### `GET /api/agent/tools`

返回当前 Agent 启动时自动发现的所有工具元数据列表，可用于前端"能力概览"展示或开发期间的接口调试。

### 3.1 请求

无请求参数。

```bash
curl http://127.0.0.1:8080/api/agent/tools
```

### 3.2 响应结构

返回 `ToolMetadata[]` 数组：

```json
[
  {
    "groupName": "UserToolGroup",
    "groupDescription": "用户管理相关工具集",
    "toolName": "queryUsers",
    "toolDescription": "根据条件查询用户列表",
    "parameters": [
      {
        "name": "department",
        "type": "String",
        "description": "部门名称",
        "required": true
      },
      {
        "name": "status",
        "type": "String",
        "description": "用户状态（active/inactive）",
        "required": false
      }
    ],
    "riskLevel": "LOW"
  },
  {
    "groupName": "UserToolGroup",
    "groupDescription": "用户管理相关工具集",
    "toolName": "deleteUser",
    "toolDescription": "根据用户 ID 删除指定用户",
    "parameters": [
      {
        "name": "userId",
        "type": "Long",
        "description": "用户 ID",
        "required": true
      }
    ],
    "riskLevel": "HIGH"
  }
]
```

**`ToolMetadata` 字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupName` | `string` | 工具组名称，对应 `@AgentTool` 注解所在 Bean 的类名。 |
| `groupDescription` | `string` | 工具组描述。 |
| `toolName` | `string` | 工具名称，对应方法名。 |
| `toolDescription` | `string` | 工具功能描述，供 LLM 进行工具选择。 |
| `parameters` | `ParamInfo[]` | 参数列表。 |
| `riskLevel` | `string` | 风险等级：`LOW`（低）、`MEDIUM`（中）、`HIGH`（高）。高风险工具在执行前会触发 `EXECUTION_CONFIRM_REQUIRED` 事件，要求用户审批。 |

**`ParamInfo` 字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `string` | 参数名称。 |
| `type` | `string` | 参数类型（Java 类型名，如 `String`、`Long`、`Integer`）。 |
| `description` | `string` | 参数描述。 |
| `required` | `boolean` | 是否必填。必填参数缺失时会触发澄清流程。 |

---

## 4. 会话管理接口

### 4.1 查询活跃会话数量

#### `GET /api/agent/sessions`

返回当前服务端维持的活跃会话数量。

**请求：** 无参数。

```bash
curl http://127.0.0.1:8080/api/agent/sessions
```

**响应示例：**

```json
{
  "count": 5
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `count` | `integer` | 当前活跃会话总数。 |

### 4.2 关闭指定会话

#### `DELETE /api/agent/sessions/{id}`

关闭指定会话并释放关联的对话记忆与资源。关闭后该 `sessionId` 将不可复用，后续请求需使用新的 `sessionId`。

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | `string` | 是 | 要关闭的会话 ID。 |

**请求示例：**

```bash
curl -X DELETE http://127.0.0.1:8080/api/agent/sessions/550e8400-e29b-41d4-a716-446655440000
```

**响应：** HTTP `200`，无响应体（`void`）。

---

## 5. 向量同步接口

> **前置条件**：仅当项目引入了 `ai-agent-vectorizer` 模块（classpath 中存在 `VectorSyncService`）时，以下接口才会自动注册。

### 5.1 手动触发向量同步

#### `POST /api/agent/vector/sync`

遍历所有已通过 `@VectorIndexed` 注册的实体，执行一轮增量同步。适用于首次接入或数据修复后的即时同步。

> **注意**：该接口为同步调用，大数据量时可能耗时较长。建议在网关层设置足够的超时时间。

**请求：** 无请求体。

```bash
curl -X POST http://127.0.0.1:8080/api/agent/vector/sync
```

**响应示例：**

```json
{
  "status": "ok"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | `string` | `"ok"` 表示本次同步已完成。 |

### 5.2 查询向量同步状态

#### `GET /api/agent/vector/status`

查询最近一次向量同步的完成时间。

```bash
curl http://127.0.0.1:8080/api/agent/vector/status
```

**响应示例：**

```json
{
  "lastSyncTime": "2026-03-11T08:00:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `lastSyncTime` | `string` | 最近一次同步完成的 ISO-8601 时间戳。若从未执行过同步则为初始时间。 |

---

## 6. SSE 事件协议详解

对话接口返回的每个 SSE 事件均为一个 `AgentEvent` JSON 对象。以下逐一说明每种事件类型的含义与载荷结构。

### 6.1 HEARTBEAT — 心跳

定期发送，用于维持 SSE 连接存活并告知客户端服务端仍在处理中。

```json
{
  "eventId": "evt-001",
  "sessionId": "s-1001",
  "type": "HEARTBEAT",
  "timestamp": "2026-03-11T08:30:00Z",
  "payload": null,
  "traceId": "trace-001"
}
```

| payload 字段 | 类型 | 说明 |
|--------------|------|------|
| — | `null` | 心跳事件无载荷。 |

> **客户端处理建议**：记录最近一次心跳时间，若超过阈值（建议 30 秒）未收到任何事件，应主动断开连接并重新发起请求。

### 6.2 THINKING_SUMMARY — 思考摘要

Agent 正在思考或推理时推送的中间摘要，供前端展示"正在思考"状态及思考过程。

```json
{
  "eventId": "evt-002",
  "sessionId": "s-1001",
  "type": "THINKING_SUMMARY",
  "timestamp": "2026-03-11T08:30:01Z",
  "payload": "正在分析您的请求，识别到需要查询用户数据...",
  "traceId": "trace-001"
}
```

| payload 字段 | 类型 | 说明 |
|--------------|------|------|
| — | `string` | 思考摘要文本。 |

### 6.3 CLARIFICATION_REQUIRED — 澄清请求

当 Agent 判断用户输入的信息不足以执行工具调用时（缺少必要槽位），推送此事件要求用户补充信息。

```json
{
  "eventId": "evt-003",
  "sessionId": "s-1001",
  "type": "CLARIFICATION_REQUIRED",
  "timestamp": "2026-03-11T08:30:01Z",
  "payload": {
    "questions": [
      {
        "slotName": "department",
        "question": "请问您要查询哪个部门的用户？",
        "type": "String",
        "required": true
      },
      {
        "slotName": "status",
        "question": "需要筛选用户状态吗？（active/inactive）",
        "type": "String",
        "required": false
      }
    ]
  },
  "traceId": "trace-001"
}
```

**payload 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `questions` | `Question[]` | 需要用户回答的问题列表。 |

**`Question` 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `slotName` | `string` | 槽位名称，客户端回传 `slotAnswers` 时需以此为键。 |
| `question` | `string` | 面向用户的自然语言问题。 |
| `type` | `string` | 期望的答案类型。 |
| `required` | `boolean` | 该槽位是否必填。 |

> **客户端处理**：收到此事件后应暂停等待用户输入，将用户回答以 `slotAnswers` 形式发送到下一次 `POST /api/agent/chat` 请求中。

### 6.4 SLOT_UPDATE — 槽位状态更新

Agent 已识别到部分槽位信息时推送，告知前端当前已收集的参数状态。

```json
{
  "eventId": "evt-004",
  "sessionId": "s-1001",
  "type": "SLOT_UPDATE",
  "timestamp": "2026-03-11T08:30:02Z",
  "payload": {
    "slots": {
      "department": "技术部",
      "status": null
    }
  },
  "traceId": "trace-001"
}
```

**payload 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `slots` | `Map<string, any>` | 当前所有槽位的键值对。值为 `null` 表示尚未填充。 |

### 6.5 EXECUTION_CONFIRM_REQUIRED — 执行确认

当 Agent 即将执行高风险工具（`riskLevel = HIGH`）时，暂停执行并推送此事件请求用户确认。

```json
{
  "eventId": "evt-005",
  "sessionId": "s-1001",
  "type": "EXECUTION_CONFIRM_REQUIRED",
  "timestamp": "2026-03-11T08:30:03Z",
  "payload": {
    "approvalToken": "appr-7890-abcd",
    "actionSummary": "即将删除用户 ID=1024（张三）",
    "riskLevel": "HIGH",
    "toolName": "deleteUser",
    "parameters": {
      "userId": 1024
    }
  },
  "traceId": "trace-001"
}
```

**payload 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `approvalToken` | `string` | 审批令牌，客户端回传 `approval.approvalId` 时需使用此值。 |
| `actionSummary` | `string` | 面向用户的操作摘要描述。 |
| `riskLevel` | `string` | 风险等级：`LOW`、`MEDIUM`、`HIGH`。 |
| `toolName` | `string` | 即将执行的工具名称。 |
| `parameters` | `Map<string, any>` | 工具调用的实际参数。 |

> **客户端处理**：向用户展示确认对话框，显示 `actionSummary` 和 `riskLevel`。用户确认/拒绝后，将 `ApprovalInfo` 放入下一次请求的 `approval` 字段中发送。

### 6.6 TOOL_TRACE — 工具调用轨迹

工具调用开始、成功或失败时推送，用于前端展示工具执行状态和调试信息。

**工具开始执行：**

```json
{
  "eventId": "evt-006a",
  "sessionId": "s-1001",
  "type": "TOOL_TRACE",
  "timestamp": "2026-03-11T08:30:04Z",
  "payload": {
    "toolName": "queryUsers",
    "toolGroup": "UserToolGroup",
    "status": "STARTED",
    "parameters": {
      "department": "技术部"
    },
    "result": null,
    "durationMs": 0,
    "errorMessage": null
  },
  "traceId": "trace-001"
}
```

**工具执行成功：**

```json
{
  "eventId": "evt-006b",
  "sessionId": "s-1001",
  "type": "TOOL_TRACE",
  "timestamp": "2026-03-11T08:30:05Z",
  "payload": {
    "toolName": "queryUsers",
    "toolGroup": "UserToolGroup",
    "status": "SUCCESS",
    "parameters": {
      "department": "技术部"
    },
    "result": [
      {"id": 1, "name": "张三"},
      {"id": 2, "name": "李四"}
    ],
    "durationMs": 120,
    "errorMessage": null
  },
  "traceId": "trace-001"
}
```

**工具执行失败：**

```json
{
  "eventId": "evt-006c",
  "sessionId": "s-1001",
  "type": "TOOL_TRACE",
  "timestamp": "2026-03-11T08:30:05Z",
  "payload": {
    "toolName": "queryUsers",
    "toolGroup": "UserToolGroup",
    "status": "FAILED",
    "parameters": {
      "department": "技术部"
    },
    "result": null,
    "durationMs": 50,
    "errorMessage": "数据库连接超时"
  },
  "traceId": "trace-001"
}
```

**payload 结构（`ToolCallInfo`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `toolName` | `string` | 工具名称。 |
| `toolGroup` | `string` | 工具所属组名。 |
| `status` | `string` | 调用状态：`STARTED`（开始）、`SUCCESS`（成功）、`FAILED`（失败）。 |
| `parameters` | `Map<string, any>` | 工具调用的入参。 |
| `result` | `any` | 工具返回值。`STARTED` 和 `FAILED` 状态下为 `null`。 |
| `durationMs` | `long` | 执行耗时（毫秒）。`STARTED` 状态下为 `0`。 |
| `errorMessage` | `string` | 错误信息。仅 `FAILED` 状态下有值。 |

### 6.7 CHART_PAYLOAD — 图表数据

当 Agent 需要以图表形式展示数据时推送。前端根据此载荷渲染可视化图表。

```json
{
  "eventId": "evt-007",
  "sessionId": "s-1001",
  "type": "CHART_PAYLOAD",
  "timestamp": "2026-03-11T08:30:06Z",
  "payload": {
    "chartType": "bar",
    "title": "各部门用户数量",
    "data": {
      "labels": ["技术部", "产品部", "运营部"],
      "datasets": [
        {
          "label": "用户数",
          "values": [42, 28, 35]
        }
      ]
    }
  },
  "traceId": "trace-001"
}
```

> 具体 payload 结构取决于业务实现，上例为典型格式。前端应根据 `chartType` 选择对应的图表渲染组件。

### 6.8 FINAL_ANSWER — 最终回答

Agent 对用户问题的最终文本回答。一个会话中可能推送多次（如分段回答）。

```json
{
  "eventId": "evt-008",
  "sessionId": "s-1001",
  "type": "FINAL_ANSWER",
  "timestamp": "2026-03-11T08:30:07Z",
  "payload": "查询结果：技术部共有 42 名用户，其中 38 人处于活跃状态。",
  "traceId": "trace-001"
}
```

| payload 字段 | 类型 | 说明 |
|--------------|------|------|
| — | `string` | 面向用户的最终回答文本。 |

### 6.9 ERROR — 错误

Agent 执行过程中发生错误时推送。

```json
{
  "eventId": "evt-009",
  "sessionId": "s-1001",
  "type": "ERROR",
  "timestamp": "2026-03-11T08:30:08Z",
  "payload": {
    "error": "TOOL_EXECUTION_FAILED",
    "message": "数据库查询超时，请稍后重试",
    "recoverable": true
  },
  "traceId": "trace-001"
}
```

**payload 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `error` | `string` | 错误码，标识错误类型。 |
| `message` | `string` | 面向用户的错误描述。 |
| `recoverable` | `boolean` | 是否可恢复。`true` 表示客户端可以重试；`false` 表示需要人工介入或更换会话。 |

### 6.10 COMPLETED — 会话完成

标志本次对话的事件流已结束。客户端收到此事件后应关闭 SSE 连接。

```json
{
  "eventId": "evt-010",
  "sessionId": "s-1001",
  "type": "COMPLETED",
  "timestamp": "2026-03-11T08:30:08Z",
  "payload": null,
  "traceId": "trace-001"
}
```

| payload 字段 | 类型 | 说明 |
|--------------|------|------|
| — | `null` | 完成事件无载荷。 |

---

## 7. 事件载荷通用结构

所有 SSE 事件共享 `AgentEvent` 统一外层结构：

```json
{
  "eventId": "string",
  "sessionId": "string",
  "type": "string",
  "timestamp": "string",
  "payload": any,
  "traceId": "string"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `eventId` | `string` | 事件唯一标识（UUID v4）。客户端应基于此字段做幂等去重。 |
| `sessionId` | `string` | 事件所属会话 ID。 |
| `type` | `string` | 事件类型枚举值，取值见 [第 6 节](#6-sse-事件协议详解)。 |
| `timestamp` | `string` | 事件产生时间，ISO-8601 格式（如 `2026-03-11T08:30:00Z`）。 |
| `payload` | `any` | 事件载荷，不同 `type` 对应不同结构。具体见各事件类型说明。 |
| `traceId` | `string` | 链路追踪 ID。同一次 `chat` 调用产生的所有事件共享相同的 `traceId`，可用于日志关联与问题排查。 |

**`AgentEventType` 枚举完整列表：**

| 枚举值 | 说明 |
|--------|------|
| `HEARTBEAT` | 心跳事件 |
| `THINKING_SUMMARY` | 思考摘要 |
| `CLARIFICATION_REQUIRED` | 需要用户澄清 |
| `SLOT_UPDATE` | 槽位状态更新 |
| `EXECUTION_CONFIRM_REQUIRED` | 需要用户确认执行 |
| `TOOL_TRACE` | 工具调用轨迹 |
| `CHART_PAYLOAD` | 图表数据载荷 |
| `FINAL_ANSWER` | 最终回答 |
| `ERROR` | 错误事件 |
| `COMPLETED` | 会话完成 |

---

## 8. 澄清式对话流程

以下是一个完整的多轮对话序列，展示从普通对话到澄清、补全、确认、执行的全流程。

### 第 1 轮：用户发起模糊请求

**请求：**

```json
POST /api/agent/chat
{
  "sessionId": "s-2001",
  "message": "删除一个用户"
}
```

**SSE 事件流：**

```
① THINKING_SUMMARY  → "正在分析您的请求..."
② CLARIFICATION_REQUIRED → 缺少必要参数，需要澄清
```

```json
{
  "type": "CLARIFICATION_REQUIRED",
  "payload": {
    "questions": [
      {
        "slotName": "userId",
        "question": "请问您要删除哪个用户？请提供用户 ID。",
        "type": "Long",
        "required": true
      }
    ]
  }
}
```

```
③ COMPLETED → 本轮事件流结束，等待用户输入
```

### 第 2 轮：用户补全槽位信息

**请求：**

```json
POST /api/agent/chat
{
  "sessionId": "s-2001",
  "slotAnswers": {
    "userId": 1024
  }
}
```

**SSE 事件流：**

```
① THINKING_SUMMARY  → "参数已补全，正在准备执行..."
② SLOT_UPDATE       → {"slots": {"userId": 1024}}
③ EXECUTION_CONFIRM_REQUIRED → 高风险操作，需要用户确认
```

```json
{
  "type": "EXECUTION_CONFIRM_REQUIRED",
  "payload": {
    "approvalToken": "appr-del-1024",
    "actionSummary": "即将删除用户 ID=1024（张三）",
    "riskLevel": "HIGH",
    "toolName": "deleteUser",
    "parameters": {"userId": 1024}
  }
}
```

```
④ COMPLETED → 本轮事件流结束，等待用户确认
```

### 第 3 轮：用户确认执行

**请求：**

```json
POST /api/agent/chat
{
  "sessionId": "s-2001",
  "approval": {
    "approvalId": "appr-del-1024",
    "approved": true,
    "comment": "确认删除该用户"
  }
}
```

**SSE 事件流：**

```
① THINKING_SUMMARY  → "用户已确认，开始执行..."
② TOOL_TRACE (STARTED)  → deleteUser 开始执行
③ TOOL_TRACE (SUCCESS)  → deleteUser 执行完成，耗时 85ms
④ FINAL_ANSWER     → "已成功删除用户张三（ID: 1024）。"
⑤ COMPLETED        → 对话完成
```

### 流程图

```
用户消息 ──→ [Agent 分析]
               │
               ├─ 参数充足 & 低风险 ──→ 直接执行 ──→ TOOL_TRACE ──→ FINAL_ANSWER ──→ COMPLETED
               │
               ├─ 参数不足 ──→ CLARIFICATION_REQUIRED ──→ COMPLETED（等待用户补全）
               │                      ↑
               │                      └── 用户发送 slotAnswers ──→ [Agent 重新分析]
               │
               └─ 参数充足 & 高风险 ──→ EXECUTION_CONFIRM_REQUIRED ──→ COMPLETED（等待用户确认）
                                              ↑
                                              └── 用户发送 approval ──→ [Agent 执行]
```

---

## 9. 错误处理

### 9.1 全局错误响应格式

所有 HTTP 错误响应均由 `AgentExceptionHandler` 统一处理，响应体格式如下：

```json
{
  "error": "ERROR_CODE",
  "message": "可读的错误描述",
  "recoverable": true
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `error` | `string` | 错误码，用于程序化判断。 |
| `message` | `string` | 面向用户/开发者的错误描述。 |
| `recoverable` | `boolean` | 是否可恢复。`true` 表示可安全重试。 |

### 9.2 异常映射规则

`AgentExceptionHandler` 仅作用于 `io.github.aiagent.starter.endpoint` 包下的控制器，映射规则如下：

| 异常类型 | HTTP 状态码 | error 值 | recoverable | 说明 |
|----------|-------------|----------|-------------|------|
| `SessionBusyException` | `409 Conflict` | `SESSION_BUSY` | `true` | 同一会话并发请求冲突。客户端应等待上一次请求完成后重试。 |
| `AgentException` | `500 Internal Server Error` | 动态（取决于具体子类的 `errorCode`） | 动态（取决于具体子类） | Agent 框架内部异常。`recoverable` 由异常实例自行决定。 |

### 9.3 SSE 流内错误

除 HTTP 级别的错误响应外，已建立的 SSE 流中也可能推送 `ERROR` 类型事件（见 [6.9 节](#69-error--错误)）。两者的区别：

| 对比维度 | HTTP 错误响应 | SSE ERROR 事件 |
|----------|--------------|----------------|
| 触发时机 | 请求阶段（连接建立前） | 对话处理过程中 |
| 传输方式 | 标准 HTTP 响应体 | SSE 事件流中的一条事件 |
| 格式 | `{"error", "message", "recoverable"}` | `AgentEvent` 包裹，payload 中含 `error`、`message`、`recoverable` |
| 处理方式 | 根据 HTTP 状态码分支处理 | 在 SSE 事件分发逻辑中处理 |

---

## 10. 前后端联调约束

### 10.1 eventId 去重

服务端为每个 SSE 事件生成唯一的 `eventId`（UUID v4）。由于网络抖动或 SSE 重连，客户端可能收到重复事件。**前端必须维护一个已处理 `eventId` 的集合**，收到事件时先检查是否已处理过，避免 UI 重复渲染。

```javascript
const processedEvents = new Set();

eventSource.onmessage = (e) => {
  const event = JSON.parse(e.data);
  if (processedEvents.has(event.eventId)) return;
  processedEvents.add(event.eventId);
  handleEvent(event);
};
```

> 建议使用 LRU 策略限制集合大小，避免内存无限增长（保留最近 1000 条即可）。

### 10.2 心跳超时检测

服务端在处理过程中定期发送 `HEARTBEAT` 事件。前端应实现超时检测机制：

1. 每收到任意事件时重置计时器。
2. 若超过 **30 秒** 未收到任何事件，判定连接异常。
3. 主动关闭当前 SSE 连接，提示用户"连接已断开"并提供重试选项。

```javascript
let heartbeatTimer = null;
const HEARTBEAT_TIMEOUT = 30000;

function resetHeartbeat() {
  clearTimeout(heartbeatTimer);
  heartbeatTimer = setTimeout(() => {
    eventSource.close();
    showReconnectPrompt();
  }, HEARTBEAT_TIMEOUT);
}

eventSource.onmessage = (e) => {
  resetHeartbeat();
  // ...处理事件
};
```

### 10.3 409 并发冲突处理

同一 `sessionId` 不允许并发对话。当用户快速连续点击发送时，第二次请求可能收到 `409 Conflict` 响应：

```json
{
  "error": "SESSION_BUSY",
  "message": "Session is busy: s-1001",
  "recoverable": true
}
```

**推荐处理策略：**

1. **前端防抖**：发送请求后禁用发送按钮，直到收到 `COMPLETED` 事件才重新启用。
2. **自动重试**：收到 409 后延迟 1-2 秒重试（最多重试 3 次）。
3. **用户提示**：若重试仍失败，提示用户"当前会话繁忙，请稍后再试"。

### 10.4 SSE 连接管理

| 注意事项 | 说明 |
|----------|------|
| 关闭代理缓冲 | 若使用 Nginx 等反向代理，需设置 `proxy_buffering off;`，否则 SSE 事件会被缓冲导致前端无法实时接收。 |
| 超时配置 | 反向代理的读超时应设置为足够大的值（建议 ≥ 300 秒），避免长耗时对话被中途断开。 |
| 跨域配置 | 若前后端分离部署，需在后端配置 CORS 允许 `text/event-stream` 响应头。 |
| 连接数限制 | 浏览器对同一域名的 SSE 并发连接数有限（HTTP/1.1 下通常为 6 个）。建议使用 HTTP/2 或保持单一活跃连接。 |

### 10.5 Nginx 参考配置

```nginx
location /api/agent/chat {
    proxy_pass http://backend;
    proxy_buffering off;
    proxy_cache off;
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    chunked_transfer_encoding off;
    proxy_read_timeout 300s;
}
```

---

## 附录：常见问题

| 问题 | 原因与解决方案 |
|------|---------------|
| SSE 事件没有实时到达，而是一次性全部返回 | 反向代理开启了缓冲。设置 `proxy_buffering off`。 |
| 收到 409 SESSION_BUSY | 同一会话存在未完成的请求。等待 `COMPLETED` 事件后再发送新请求。 |
| 向量同步接口 404 | 未引入 `ai-agent-vectorizer` 模块。该接口仅在 classpath 中存在 `VectorSyncService` 时自动注册。 |
| 长时间无事件推送 | 检查后端日志是否有异常。前端应通过心跳超时检测机制自动处理。 |
| `slotAnswers` 发送后未生效 | 确认键名与 `CLARIFICATION_REQUIRED` 事件中 `questions[].slotName` 完全一致（区分大小写）。 |
| `approval` 发送后被拒绝 | 确认 `approvalId` 与 `EXECUTION_CONFIRM_REQUIRED` 事件中的 `approvalToken` 一致且未过期。 |
