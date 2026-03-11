package io.github.aiagent.vectorizer;

import java.util.List;

/**
 * 向量同步实体注册接口。
 * <p>
 * 业务模块需实现此接口并注册为 Spring Bean，返回所有需要参与向量同步的实体类。
 * 这些实体类必须标注 {@link io.github.aiagent.vectorizer.annotation.VectorIndexed} 注解。
 * <p>
 * 同步调度器 {@link VectorSyncScheduler} 会自动发现所有 Provider 实例，
 * 遍历其返回的实体类执行增量同步。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @Component
 * public class MyVectorSyncEntityProvider implements VectorSyncEntityProvider {
 *     @Override
 *     public List<Class<?>> entities() {
 *         return List.of(User.class, Product.class);
 *     }
 * }
 * }</pre>
 *
 * @see VectorSyncScheduler
 */
public interface VectorSyncEntityProvider {

    /**
     * 返回需要参与向量同步的实体类列表。
     * <p>
     * 列表中的每个类都应标注 {@code @VectorIndexed} 注解，
     * 未标注的类在同步时会被跳过。
     *
     * @return 实体类列表，不可为 null
     */
    List<Class<?>> entities();
}
