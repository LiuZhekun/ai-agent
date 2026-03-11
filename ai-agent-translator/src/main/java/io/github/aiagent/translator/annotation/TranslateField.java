package io.github.aiagent.translator.annotation;

import io.github.aiagent.translator.strategy.TranslationPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段翻译注解 —— 声明 DTO 字段的自动翻译规则。
 * <p>
 * 本注解存在两条使用路径：
 * <ul>
 *   <li><b>路径 A（装饰器模式）</b>：{@link io.github.aiagent.translator.decorator.TranslateToolCallbackDecorator}
 *       在工具调用前扫描 DTO 上的注解，自动完成"自然语言 → 系统值"的翻译。</li>
 *   <li><b>路径 B（LLM 工具模式）</b>：LLM 在对话中主动调用
 *       {@link io.github.aiagent.translator.tool.TranslateTools} 提供的翻译工具，
 *       根据注解的元信息构造翻译请求。</li>
 * </ul>
 *
 * <p>示例用法（标注在 DTO 字段上）：
 * <pre>{@code
 * @TranslateField(type = "DICT", source = "statusName", target = "user_status")
 * private String status;
 * }</pre>
 *
 * @see io.github.aiagent.translator.decorator.TranslateToolCallbackDecorator
 * @see io.github.aiagent.translator.tool.TranslateTools
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TranslateField {

    /** 翻译器类型，对应 {@link io.github.aiagent.translator.FieldTranslator#getType()} 的返回值（如 {@code "DICT"}、{@code "ENTITY_REF"}）。 */
    String type();

    /** DTO 中提供原始值的字段名（即"从哪个字段取值"）。 */
    String source();

    /** 翻译目标标识（字典类型编码 / 实体表名）。 */
    String target();

    /** 在目标数据源中用于匹配的列名，默认 {@code "name"}。 */
    String lookupField() default "name";

    /** 翻译结果所在的列名，默认 {@code "id"}。 */
    String resultField() default "id";

    /** 匹配到多条记录时的冲突处理策略。 */
    TranslationPolicy.MultiResultPolicy multiResultPolicy() default TranslationPolicy.MultiResultPolicy.FIRST_MATCH;

    /** 未匹配到记录时的处理策略。 */
    TranslationPolicy.NotFoundPolicy notFoundPolicy() default TranslationPolicy.NotFoundPolicy.FAIL;
}
