package io.github.aiagent.core.prompt;

import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.memory.MemoryConfig;
import io.github.aiagent.core.model.ChatMessage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 系统提示词（System Prompt）构建器，负责加载模板并注入动态变量，生成完整的系统指令。
 * <p>
 * 工作机制：
 * <ol>
 *   <li>从 classpath 加载 Prompt 模板文件（{@code templates/agent-system.txt}）</li>
 *   <li>从 {@link AgentSession#getMetadata()} 中提取动态变量（知识片段、Schema 描述、
 *       可用工具、对话历史等）</li>
 *   <li>使用简单的字符串占位符替换（{@code {variable_name}} 格式）将变量注入模板</li>
 * </ol>
 * <p>
 * 模板中支持的占位符：
 * <ul>
 *   <li>{@code {knowledge_snippets}} —— 向量检索召回的相关知识片段</li>
 *   <li>{@code {schema_description}} —— 数据源 Schema 描述信息</li>
 *   <li>{@code {available_tools}} —— 当前可用的工具列表</li>
 *   <li>{@code {conversation_history}} —— 格式化后的对话历史</li>
 *   <li>{@code {safety_rules}} —— 安全规则与运行时配置信息</li>
 * </ul>
 * <p>
 * 选择简单字符串替换而非模板引擎（如 Thymeleaf），是因为 Prompt 模板结构简单、
 * 变量数量有限，无需引入额外依赖。
 *
 * @see AgentSession#getMetadata() 动态变量的数据来源
 */
@Component
public class SystemPromptBuilder {

    private final MemoryConfig memoryConfig;
    private final ResourceLoader resourceLoader;

    public SystemPromptBuilder(MemoryConfig memoryConfig, ResourceLoader resourceLoader) {
        this.memoryConfig = memoryConfig;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 加载模板并注入会话上下文变量，构建完整的系统 Prompt。
     *
     * @param session 当前 Agent 会话，从其 metadata 中提取动态变量
     * @return 完整的系统 Prompt 字符串
     * @throws IllegalStateException 模板文件加载失败时抛出
     */
    public String build(AgentSession session) {
        String template = loadTemplate("classpath:templates/agent-system.txt");
        Map<String, String> variables = Map.of(
                "{knowledge_snippets}", getString(session, "knowledgeSnippets"),
                "{schema_description}", getString(session, "schemaDescription"),
                "{available_tools}", getString(session, "availableTools"),
                "{conversation_history}", getString(session, "conversationHistory"),
                "{safety_rules}", "memory.strategy=" + memoryConfig.getStrategy()
        );
        String prompt = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            prompt = prompt.replace(entry.getKey(), entry.getValue());
        }
        return prompt;
    }

    /**
     * 将对话历史列表格式化为 "ROLE: content" 格式的纯文本，供注入 Prompt 的
     * {@code {conversation_history}} 占位符。
     *
     * @param history 对话历史列表
     * @return 格式化后的文本；输入为空时返回空字符串
     */
    public static String formatHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : history) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append('\n');
        }
        return sb.toString();
    }

    private String getString(AgentSession session, String key) {
        return String.valueOf(session.getMetadata().getOrDefault(key, ""));
    }

    private String loadTemplate(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Load prompt template failed: " + path, ex);
        }
    }
}
