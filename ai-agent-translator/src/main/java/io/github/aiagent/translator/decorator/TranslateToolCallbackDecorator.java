package io.github.aiagent.translator.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.aiagent.core.tool.ToolCallbackInterceptor;
import io.github.aiagent.translator.TranslateContext;
import io.github.aiagent.translator.TranslatorRegistry;
import io.github.aiagent.translator.strategy.TranslationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路径 A：工具调用前的翻译拦截器（装饰器模式）。
 * <p>
 * <b>核心机制</b>：实现 {@link ToolCallbackInterceptor}，在工具真正执行前的
 * {@link #beforeCall} 阶段解析工具输入 JSON，尝试将"自然语言字段"翻译为
 * 系统可执行值（如字典 code、实体 id）。翻译失败不会阻断工具执行，保证主流程鲁棒性。
 * <p>
 * 翻译触发有两种方式：
 * <ol>
 *   <li><b>显式规则</b> — 工具输入中包含 {@code _translate} 配置块，指定字段的翻译类型和目标。</li>
 *   <li><b>启发式规则</b> — 根据字段命名约定自动识别（如以 {@code DictName} 结尾的字段、
 *       {@code deptName} / {@code userName} 等已知实体引用字段）。</li>
 * </ol>
 * <p>
 * 翻译结果会通过 {@link #applyTranslations} 回写到工具的实际入参 JSON 中，
 * 使得工具执行时直接使用翻译后的值（如 deptName="技术部" → deptId=3）。
 * 同时翻译快照保存在 {@code ThreadLocal} 和工具级快照中供调试和审计使用。
 *
 * @see ToolCallbackInterceptor
 * @see TranslatorRegistry
 */
@Component
public class TranslateToolCallbackDecorator implements ToolCallbackInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TranslateToolCallbackDecorator.class);
    private static final ThreadLocal<Map<String, Object>> PRE_TRANSLATED = new ThreadLocal<>();
    private static final Map<String, Map<String, Object>> SNAPSHOT_BY_TOOL = new ConcurrentHashMap<>();

    private final TranslatorRegistry translatorRegistry;
    private final ObjectMapper objectMapper;

    public TranslateToolCallbackDecorator(TranslatorRegistry translatorRegistry, ObjectMapper objectMapper) {
        this.translatorRegistry = translatorRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public String beforeCall(String toolName, String toolInput) {
        Map<String, Object> translated = new LinkedHashMap<>();
        PRE_TRANSLATED.set(translated);
        if (toolInput == null || toolInput.isBlank()) {
            return toolInput;
        }
        try {
            JsonNode root = objectMapper.readTree(toolInput);
            if (!root.isObject()) {
                return toolInput;
            }
            processExplicitTranslateConfig(root, translated);
            processHeuristicFields(root, translated);
            if (!translated.isEmpty()) {
                SNAPSHOT_BY_TOOL.put(toolName, Map.copyOf(translated));
                log.info("Tool pre-translation finished, tool={}, translated={}", toolName, translated);
                return applyTranslations(toolInput, translated);
            }
        } catch (Exception ex) {
            log.warn("Tool pre-translation skipped, tool={}, reason={}", toolName, ex.getMessage());
        }
        return toolInput;
    }

    /**
     * 将翻译结果回写到工具输入 JSON 中。
     * <p>
     * 对于每个翻译结果，在原始 JSON 中找到对应的源字段名，
     * 将翻译后的值写入。例如 deptName="技术部" 翻译得到 deptId=3，
     * 则在 JSON 中设置 deptName 的值为翻译结果（或新增翻译目标字段）。
     */
    @SuppressWarnings("unchecked")
    private String applyTranslations(String toolInput, Map<String, Object> translated) {
        try {
            Map<String, Object> inputMap = objectMapper.readValue(toolInput, Map.class);
            for (Map.Entry<String, Object> entry : translated.entrySet()) {
                inputMap.put(entry.getKey(), entry.getValue());
            }
            inputMap.remove("_translate");
            return objectMapper.writeValueAsString(inputMap);
        } catch (Exception ex) {
            log.warn("Failed to apply translations to tool input, using original input. reason={}", ex.getMessage());
            return toolInput;
        }
    }

    @Override
    public String afterCall(String toolName, String toolInput, String toolOutput) {
        PRE_TRANSLATED.remove();
        return toolOutput;
    }

    /**
     * 对单个值执行翻译，供显式规则和启发式规则复用。
     */
    public Object translate(String type, Object value, String target) {
        return translate(type, value, target, null, null,
                TranslationPolicy.MultiResultPolicy.FIRST_MATCH,
                TranslationPolicy.NotFoundPolicy.FAIL);
    }

    public Object translate(String type, Object value, String target, String lookupField, String resultField,
                            TranslationPolicy.MultiResultPolicy multiResultPolicy,
                            TranslationPolicy.NotFoundPolicy notFoundPolicy) {
        TranslateContext context = new TranslateContext();
        context.setSourceValue(value);
        context.setTarget(target);
        context.setLookupField(lookupField);
        context.setResultField(resultField);
        context.setMultiResultPolicy(multiResultPolicy);
        context.setNotFoundPolicy(notFoundPolicy);
        context.setMetadata(Map.of());
        return translatorRegistry.get(type).translate(context);
    }

    /**
     * 获取某个工具最近一次调用的翻译快照，常用于调试与测试断言。
     */
    public Map<String, Object> getLastToolTranslations(String toolName) {
        return SNAPSHOT_BY_TOOL.getOrDefault(toolName, Map.of());
    }

    private void processExplicitTranslateConfig(JsonNode root, Map<String, Object> translated) {
        JsonNode configNode = root.get("_translate");
        if (configNode == null || !configNode.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = configNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String sourceKey = entry.getKey();
            JsonNode rule = entry.getValue();
            if (!rule.isObject()) {
                continue;
            }
            String type = text(rule, "type", null);
            String target = text(rule, "target", null);
            String lookupField = text(rule, "lookupField", null);
            String resultField = text(rule, "resultField", null);
            TranslationPolicy.MultiResultPolicy multiResultPolicy =
                    enumOrDefault(text(rule, "multiResultPolicy", null),
                            TranslationPolicy.MultiResultPolicy.class,
                            TranslationPolicy.MultiResultPolicy.FIRST_MATCH);
            TranslationPolicy.NotFoundPolicy notFoundPolicy =
                    enumOrDefault(text(rule, "notFoundPolicy", null),
                            TranslationPolicy.NotFoundPolicy.class,
                            TranslationPolicy.NotFoundPolicy.FAIL);
            if (type == null || target == null) {
                continue;
            }
            JsonNode sourceNode = root.get(sourceKey);
            if (sourceNode == null || sourceNode.isNull()) {
                continue;
            }
            Object result = translate(type, sourceNode.asText(), target, lookupField, resultField, multiResultPolicy, notFoundPolicy);
            translated.put(sourceKey, result);
        }
    }

    private void processHeuristicFields(JsonNode root, Map<String, Object> translated) {
        Map<String, String> entityRefTargets = new HashMap<>();
        entityRefTargets.put("deptName", "sys_department");
        entityRefTargets.put("departmentName", "sys_department");
        entityRefTargets.put("userName", "sys_user");

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value == null || value.isNull() || !value.isTextual()) {
                continue;
            }
            String text = value.asText().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (key.endsWith("DictName")) {
                String prefix = key.substring(0, key.length() - "DictName".length());
                JsonNode dictTypeNode = root.get(prefix + "DictType");
                if (dictTypeNode != null && dictTypeNode.isTextual()) {
                    Object code = translate("DICT", text, dictTypeNode.asText());
                    translated.put(key, code);
                }
                continue;
            }
            String entityTarget = entityRefTargets.get(key);
            if (entityTarget != null) {
                Object id = translate("ENTITY_REF", text, entityTarget);
                translated.put(key, id);
            }
        }
    }

    private String text(JsonNode node, String key, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String result = value.asText();
        return Objects.requireNonNullElse(result, defaultValue);
    }

    private <T extends Enum<T>> T enumOrDefault(String value, Class<T> enumType, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
