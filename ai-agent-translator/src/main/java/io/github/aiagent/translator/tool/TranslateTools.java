package io.github.aiagent.translator.tool;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.translator.TranslateContext;
import io.github.aiagent.translator.TranslatorRegistry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 路径 B：LLM 主动调用的翻译工具。
 */
@Component
@AgentTool(name = "translate-tools", description = "翻译工具组")
public class TranslateTools {

    private final TranslatorRegistry translatorRegistry;

    public TranslateTools(TranslatorRegistry translatorRegistry) {
        this.translatorRegistry = translatorRegistry;
    }

    @Tool(description = "字典值翻译")
    public Object findDictCode(String dictType, String dictName) {
        TranslateContext context = new TranslateContext();
        context.setTarget(dictType);
        context.setSourceValue(dictName);
        context.setMetadata(Map.of());
        return translatorRegistry.get("DICT").translate(context);
    }

    @Tool(description = "实体引用翻译")
    public Object findEntityId(String tableName, String name) {
        TranslateContext context = new TranslateContext();
        context.setTarget(tableName);
        context.setSourceValue(name);
        context.setMetadata(Map.of());
        return translatorRegistry.get("ENTITY_REF").translate(context);
    }
}
