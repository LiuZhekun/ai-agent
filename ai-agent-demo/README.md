# ai-agent-demo

## 模块目的

演示如何在业务项目接入 Starter，并完成工具、知识、翻译、SSE 全链路联调。

## 前置条件

- 已启动 MySQL/Redis/Milvus（可用 `deploy/install-all.sh`）。
- 已配置 LLM Key（DashScope 或 DeepSeek）。

## 启动步骤

```bash
mvn -pl ai-agent-demo -am spring-boot:run
```

## 示例请求

```bash
curl -N -X POST "http://127.0.0.1:8080/api/agent/chat" \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"demo-1\",\"message\":\"查询技术部用户\"}"
```

## 典型场景

- 读取类：查询用户、查询部门、查询字典。
- 写入类：新增/修改/删除用户（带确认流程）。
- 翻译类：部门名称 -> 部门 ID、字典名称 -> 字典编码。
