package io.github.aiagent.translator.strategy;

/**
 * 翻译冲突策略。
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
