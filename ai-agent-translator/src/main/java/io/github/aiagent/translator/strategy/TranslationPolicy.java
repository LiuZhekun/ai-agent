package io.github.aiagent.translator.strategy;

/**
 * 翻译冲突策略 —— 定义翻译过程中"多条匹配"和"未匹配"两种异常情况的处理方式。
 * <p>
 * 翻译器（如 {@link io.github.aiagent.translator.builtin.DictTranslator}）在查询数据源后，
 * 根据 {@link MultiResultPolicy} 和 {@link NotFoundPolicy} 的取值决定是抛异常、返回首条结果
 * 还是保留原值等。调用方可通过 {@link io.github.aiagent.translator.annotation.TranslateField}
 * 注解或 {@link io.github.aiagent.translator.TranslateContext} 设置策略。
 */
public class TranslationPolicy {
    public enum MultiResultPolicy {
        /** 交由用户选择候选结果。 */
        USER_SELECT,
        /** 使用首个匹配结果。 */
        FIRST_MATCH,
        /** 直接失败并抛错。 */
        FAIL
    }

    public enum NotFoundPolicy {
        /** 未命中时失败。 */
        FAIL,
        /** 未命中时跳过该字段。 */
        SKIP,
        /** 未命中时保留原值。 */
        USE_ORIGINAL
    }
}
