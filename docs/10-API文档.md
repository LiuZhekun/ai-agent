# API 文档

## 目的

描述后端 REST/SSE 接口契约，便于前后端联调与外部接入。

## 前置条件

- 服务已启动（默认 `http://127.0.0.1:8080`）
- 请求头 `Content-Type: application/json`

## 接口列表

### `POST /api/agent/chat`

- 说明：发起对话并返回 SSE 事件流。
- 入参示例：

```json
{"sessionId":"s-1001","message":"查询技术部用户"}
```

### `GET /api/agent/tools`

- 说明：获取当前可用工具及参数元数据。

### `GET /api/agent/sessions`

- 说明：获取活跃会话列表。

### `DELETE /api/agent/sessions/{id}`

- 说明：关闭指定会话。

### `POST /api/agent/vector/sync`

- 说明：触发向量全量同步。

## SSE 事件类型

- `HEARTBEAT`
- `THINKING_SUMMARY`
- `CLARIFICATION_REQUIRED`
- `SLOT_UPDATE`
- `EXECUTION_CONFIRM_REQUIRED`
- `TOOL_TRACE`
- `CHART_PAYLOAD`
- `FINAL_ANSWER`
- `ERROR`
- `COMPLETED`

## 常见问题

- SSE 被网关缓冲：关闭代理缓冲（如 Nginx `proxy_buffering off`）。
- 会话忙：同一会话并发请求会返回冲突提示，应切换新会话或重试。
