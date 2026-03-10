package io.github.aiagent.core.prompt;

import io.github.aiagent.core.agent.AgentSession;
import io.github.aiagent.core.memory.MemoryConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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
     * 组装系统 Prompt。
     */
    public String build(AgentSession session) {
        String template = loadTemplate("classpath:templates/agent-system.txt");
        Map<String, String> variables = Map.of(
                "{knowledge_snippets}", String.valueOf(session.getMetadata().getOrDefault("knowledgeSnippets", "")),
                "{schema_description}", String.valueOf(session.getMetadata().getOrDefault("schemaDescription", "")),
                "{available_tools}", String.valueOf(session.getMetadata().getOrDefault("availableTools", "")),
                "{safety_rules}", "memory.strategy=" + memoryConfig.getStrategy()
        );
        String prompt = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            prompt = prompt.replace(entry.getKey(), entry.getValue());
        }
        return prompt;
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
