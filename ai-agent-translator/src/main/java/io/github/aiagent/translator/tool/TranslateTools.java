package io.github.aiagent.translator.tool;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.translator.TranslateContext;
import io.github.aiagent.translator.TranslatorRegistry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 路径 B：LLM 主动调用的翻译工具。
 * <p>
 * 与路径 A（{@link io.github.aiagent.translator.decorator.TranslateToolCallbackDecorator 装饰器}
 * 自动拦截）不同，路径 B 将翻译能力作为 LLM 可调用的工具暴露出来。
 * 当 LLM 在对话过程中识别到需要翻译的字段时，会主动调用本类提供的方法完成翻译。
 * <p>
 * 本类通过 {@link AgentTool} 注解注册为工具组，每个 {@link Tool} 方法对应一种翻译能力：
 * <ul>
 *   <li>{@link #findDictCode} — 字典名称 → 字典编码</li>
 *   <li>{@link #findEntityId} — 实体名称 → 实体主键 ID</li>
 * </ul>
 *
 * @see io.github.aiagent.translator.TranslatorRegistry
 */
@Component
@AgentTool(name = "translate-tools", description = "翻译工具组")
public class TranslateTools {

    private final TranslatorRegistry translatorRegistry;

    public TranslateTools(TranslatorRegistry translatorRegistry) {
        this.translatorRegistry = translatorRegistry;
    }

    /**
     * 将字典名称翻译为对应的字典编码。
     *
     * @param dictType 字典类型（对应 {@code sys_dict.type}）
     * @param dictName 字典项名称（如 {@code "启用"}）
     * @return 匹配到的字典编码值
     */
    @Tool(description = "字典值翻译")
    public Object findDictCode(String dictType, String dictName) {
        TranslateContext context = new TranslateContext();
        context.setTarget(dictType);
        context.setSourceValue(dictName);
        context.setMetadata(Map.of());
        return translatorRegistry.get("DICT").translate(context);
    }

    /**
     * 将实体名称翻译为对应的主键 ID。
     *
     * @param tableName 目标实体表名（需在白名单内，如 {@code "sys_department"}）
     * @param name      实体名称（如 {@code "研发部"}）
     * @return 匹配到的实体主键 ID
     */
    @Tool(description = "实体引用翻译")
    public Object findEntityId(String tableName, String name) {
        TranslateContext context = new TranslateContext();
        context.setTarget(tableName);
        context.setSourceValue(name);
        context.setMetadata(Map.of());
        return translatorRegistry.get("ENTITY_REF").translate(context);
    }
}
