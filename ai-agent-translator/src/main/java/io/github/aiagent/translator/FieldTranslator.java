package io.github.aiagent.translator;

/**
 * 字段翻译器的 SPI（Service Provider Interface）。
 * <p>
 * 翻译模块采用策略模式：每种翻译类型（字典、实体引用等）实现本接口，
 * 并通过 Spring 自动注入到 {@link TranslatorRegistry} 中完成注册。
 * 扩展新的翻译类型只需新增一个实现类并标注 {@code @Component}。
 *
 * @see TranslatorRegistry
 * @see TranslateContext
 */
public interface FieldTranslator {

    /**
     * 返回翻译器的类型标识（如 {@code "DICT"}、{@code "ENTITY_REF"}），
     * 该标识会作为路由键在 {@link TranslatorRegistry} 中索引本实例。
     *
     * @return 全局唯一的翻译器类型字符串
     */
    String getType();

    /**
     * 根据上下文执行翻译逻辑，将源值转换为目标系统可识别的值。
     *
     * @param ctx 翻译上下文，包含源值、目标表/字典、查询字段及冲突策略等信息
     * @return 翻译后的值；具体类型取决于实现（如字典 code、实体 id 等）
     */
    Object translate(TranslateContext ctx);
}
