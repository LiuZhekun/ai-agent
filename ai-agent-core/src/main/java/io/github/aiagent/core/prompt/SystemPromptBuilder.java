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
 * 系统提示词构建器。
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
     * 组装系统 Prompt（含知识、Schema、对话历史、安全规则）。
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
     * 将对话历史列表格式化为文本，供注入 Prompt。
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
