package io.github.aiagent.translator.annotation;

import io.github.aiagent.translator.strategy.TranslationPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段翻译注解。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TranslateField {
    String type();
    String source();
    String target();
    String lookupField() default "name";
    String resultField() default "id";
    TranslationPolicy.MultiResultPolicy multiResultPolicy() default TranslationPolicy.MultiResultPolicy.FIRST_MATCH;
    TranslationPolicy.NotFoundPolicy notFoundPolicy() default TranslationPolicy.NotFoundPolicy.FAIL;
}
