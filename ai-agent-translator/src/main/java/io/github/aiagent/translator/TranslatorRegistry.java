package io.github.aiagent.translator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 翻译器注册中心。
 * 通过 type（如 DICT、ENTITY_REF）路由到具体翻译器实现。
 */
@Component
public class TranslatorRegistry {

    private final Map<String, FieldTranslator> translators;

    public TranslatorRegistry(List<FieldTranslator> translators) {
        this.translators = translators.stream().collect(Collectors.toMap(FieldTranslator::getType, Function.identity()));
    }

    /**
     * 按翻译类型获取实现，不存在时抛异常以便调用方尽早感知配置问题。
     */
    public FieldTranslator get(String type) {
        FieldTranslator translator = translators.get(type);
        if (translator == null) {
            throw new IllegalArgumentException("Translator not found: " + type);
        }
        return translator;
    }
}
