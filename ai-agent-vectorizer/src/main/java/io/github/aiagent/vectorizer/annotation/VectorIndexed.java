package io.github.aiagent.vectorizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明某个实体可被向量同步模块索引。
 * <p>
 * 标注在实体类上后，{@link io.github.aiagent.vectorizer.VectorSyncService} 会读取该注解配置，
 * 从实体对应的数据库表中提取指定字段的文本，向量化后写入 Milvus。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @TableName("sys_user")
 * @VectorIndexed(
 *     collection = "user_profile",
 *     fields = {"name", "phone", "email"},
 *     textTemplate = "姓名:{name} 手机号:{phone} 邮箱:{email}"
 * )
 * public class User {
 *     private Long id;
 *     private String name;
 *     private String phone;
 *     private String email;
 * }
 * }</pre>
 * <p>
 * <b>注意：</b>实体类还需通过 {@link io.github.aiagent.vectorizer.VectorSyncEntityProvider}
 * 注册后才会参与同步调度。
 *
 * @see io.github.aiagent.vectorizer.VectorSyncService
 * @see io.github.aiagent.vectorizer.VectorSyncEntityProvider
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorIndexed {

    /**
     * 目标 Milvus Collection 名称。
     * <p>
     * 建议按业务域命名，如 {@code "user_profile"}、{@code "product_catalog"}。
     * 同一个 Collection 中的向量可在检索时被统一召回。
     */
    String collection();

    /**
     * 参与向量化的实体字段名（Java 属性名，非数据库列名）。
     * <p>
     * 框架会通过反射校验字段是否在实体类中存在，不存在的字段会被跳过。
     * 同时会不区分大小写地匹配到数据库中的实际列名。
     */
    String[] fields();

    /**
     * 文本拼装模板，支持 {@code {fieldName}} 占位符。
     * <p>
     * 示例：{@code "姓名:{name}\n手机号:{phone}"}
     * <p>
     * 为空时按默认规则拼接：{@code 字段名: 值}（每个字段占一行）。
     * 自定义模板通常能提供更好的语义质量，推荐使用。
     */
    String textTemplate() default "";
}
