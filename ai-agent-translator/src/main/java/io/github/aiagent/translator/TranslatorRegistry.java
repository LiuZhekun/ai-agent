package io.github.aiagent.translator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 翻译器注册中心 —— 基于 Spring 依赖注入实现自动发现与路由。
 * <p>
 * 启动时，Spring 会将容器中所有 {@link FieldTranslator} 实例以列表形式注入，
 * 本类在构造时按 {@link FieldTranslator#getType()} 建立 {@code type → translator} 映射表。
 * 调用方只需提供类型字符串即可获取对应的翻译器，无需关心具体实现类。
 * <p>
 * 扩展方式：新增一个实现了 {@link FieldTranslator} 的 {@code @Component}，
 * 它会被 Spring 自动收集并注册到本中心。
 *
 * @see FieldTranslator
 */
@Component
public class TranslatorRegistry {

    private final Map<String, FieldTranslator> translators;

    public TranslatorRegistry(List<FieldTranslator> translators) {
        this.translators = translators.stream().collect(Collectors.toMap(FieldTranslator::getType, Function.identity()));
    }

    /**
     * 按翻译类型获取对应的翻译器实例。
     *
     * @param type 翻译器类型标识（如 {@code "DICT"}、{@code "ENTITY_REF"}）
     * @return 对应的 {@link FieldTranslator} 实例
     * @throws IllegalArgumentException 类型未注册时抛出，帮助调用方尽早发现配置问题
     */
    public FieldTranslator get(String type) {
        FieldTranslator translator = translators.get(type);
        if (translator == null) {
            throw new IllegalArgumentException("Translator not found: " + type);
        }
        return translator;
    }
}
