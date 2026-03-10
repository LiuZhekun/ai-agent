package io.github.aiagent.vectorizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明某个实体可被向量同步模块索引。
 * <p>
 * 向量同步时会读取该注解配置，按实体对应表抽取字段文本并写入向量库。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorIndexed {
    /**
     * 目标向量集合名称。
     */
    String collection();

    /**
     * 参与向量化的文本字段名（实体字段名）。
     */
    String[] fields();

    /**
     * 文本拼装模板。
     * <p>
     * 为空时按 {@code 字段名: 值} 自动拼接；非空时可用 {@code {field}} 占位。
     */
    String textTemplate() default "";
}
