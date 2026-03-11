package io.github.aiagent.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Agent Demo 启动类 —— 演示如何在业务项目中接入 ai-agent-spring-boot-starter。
 *
 * <h2>快速理解</h2>
 * <p>本 Demo 模拟一个包含"用户 / 部门 / 字典"的典型业务系统，展示如何：</p>
 * <ol>
 *   <li>定义 <b>Agent 工具</b>（{@code @AgentTool} + {@code @Tool}）：让 AI 可以调用业务方法（查询用户、新增部门等）</li>
 *   <li>使用 <b>字段翻译</b>（{@code @TranslateField}）：自动将用户输入的"部门名称"翻译为数据库中的部门 ID</li>
 *   <li>使用 <b>向量同步</b>（{@code @VectorIndexed}）：将业务数据向量化存入 Milvus，支持语义搜索</li>
 *   <li>通过 <b>SSE</b> 接口与前端实时交互</li>
 * </ol>
 *
 * <h2>关键注解说明</h2>
 * <ul>
 *   <li>{@code @SpringBootApplication} — Spring Boot 自动配置；ai-agent-spring-boot-starter 通过 auto-configuration 自动注册 Agent 能力</li>
 *   <li>{@code @EnableScheduling} — 开启定时任务调度，向量同步模块的增量同步依赖此注解</li>
 *   <li>{@code @MapperScan} — 扫描 MyBatis-Plus Mapper 接口所在包，使其被 Spring 管理</li>
 * </ul>
 *
 * <h2>启动方式</h2>
 * <pre>{@code
 * # 使用 DashScope（通义千问）
 * mvn -pl ai-agent-demo -am spring-boot:run -Dspring-boot.run.profiles=dashscope
 *
 * # 使用 DeepSeek
 * mvn -pl ai-agent-demo -am spring-boot:run -Dspring-boot.run.profiles=deepseek
 * }</pre>
 *
 * @see io.github.aiagent.demo.tools.UserTools         工具定义示例
 * @see io.github.aiagent.demo.config.DemoAgentConfig   业务配置示例
 * @see io.github.aiagent.demo.config.DemoVectorSyncEntityProvider 向量同步注册示例
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("io.github.aiagent.demo.mapper")
public class AiAgentDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAgentDemoApplication.class, args);
    }
}
