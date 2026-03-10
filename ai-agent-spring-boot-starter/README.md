# ai-agent-spring-boot-starter

## 模块目的

对外提供一站式自动装配，业务项目只需引入 Starter 和基础配置即可启用 Agent 能力。

## 自动装配内容

- 核心引擎：`AgentEngine`、会话管理、Advisor 链。
- 可选模块：knowledge / vectorizer / translator（按类路径条件装配）。
- Web 端点：对话、工具、会话、向量同步接口。

## 关键配置

```properties
ai.agent.llm.provider=dashscope
ai.agent.memory.strategy=TOKEN_WINDOW
ai.agent.thinking.level=SUMMARY
ai.agent.sse.heartbeat-seconds=10
```

## 条件装配说明

- 引入 `ai-agent-knowledge` 时启用知识增强配置。
- 引入 `ai-agent-vectorizer` 时启用向量同步能力。
- 引入 `ai-agent-translator` 时启用翻译拦截器链。

## 建议

- 在业务工程统一管理 `application-*.yml` 的多环境配置。
- 通过 Spring Boot Actuator 暴露健康检查和指标。
