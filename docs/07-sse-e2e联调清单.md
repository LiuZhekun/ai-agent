# SSE 端到端联调清单

## 事件类型校验
- heartbeat
- thinking_summary
- tool_trace
- final_answer
- chart_payload
- error
- completed
- clarification_required
- slot_update
- execution_confirm_required

## 场景校验
1. 查询用户触发澄清并补全后返回结果。
2. 新增用户出现翻译冲突，用户选择后确认执行。
3. 心跳中断后自动重连且 eventId 去重。
4. 同会话并发请求第二个返回 409。
5. traceId 在 SSE 与审计日志保持一致。
