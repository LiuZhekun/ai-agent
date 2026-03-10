package io.github.aiagent.translator;

/**
 * 字段翻译器 SPI。
 */
public interface FieldTranslator {
    String getType();
    Object translate(TranslateContext ctx);
}
