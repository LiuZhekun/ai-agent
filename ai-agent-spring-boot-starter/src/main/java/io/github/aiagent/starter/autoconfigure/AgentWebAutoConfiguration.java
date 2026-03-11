package io.github.aiagent.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Web 接口自动装配 —— 自动注册 Agent 提供的 REST / SSE 端点。
 * <p>
 * 扫描 {@code io.github.aiagent.starter.endpoint} 包，注册以下控制器：
 * <ul>
 *   <li>{@link io.github.aiagent.starter.endpoint.AgentChatController} — SSE 流式对话</li>
 *   <li>{@link io.github.aiagent.starter.endpoint.AgentToolController} — 工具元数据查询</li>
 *   <li>{@link io.github.aiagent.starter.endpoint.AgentSessionController} — 会话管理</li>
 *   <li>{@link io.github.aiagent.starter.endpoint.AgentVectorController} — 向量同步管理（按需）</li>
 *   <li>{@link io.github.aiagent.starter.endpoint.AgentExceptionHandler} — 全局异常处理</li>
 * </ul>
 */
@AutoConfiguration
@ComponentScan(basePackages = "io.github.aiagent.starter.endpoint")
public class AgentWebAutoConfiguration {
}
