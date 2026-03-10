# ai-agent-ui

## 模块目的

提供 Agent 对话控制台，支持 SSE 流式渲染、工具轨迹、思考摘要、图表展示与多会话管理。

## 前置条件

- Node.js 18+
- 后端服务可访问（默认 `http://127.0.0.1:8080`）

## 启动命令

```bash
npm install
npm run dev
```

## 目录说明

- `src/api`：SSE 对接封装。
- `src/stores`：会话与消息状态管理（Pinia）。
- `src/components`：消息气泡、工具时间线、图表、Markdown 渲染等组件。

## 联调提示

- 若出现 SSE 中断，先检查后端心跳事件是否持续发送。
- 若跨域报错，检查 `vite.config.ts` 代理配置。
