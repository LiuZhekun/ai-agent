package io.github.aiagent.demo.config;

import io.github.aiagent.demo.entity.User;
import io.github.aiagent.vectorizer.VectorSyncEntityProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量同步实体注册 —— 告诉框架"哪些实体需要同步到向量数据库"。
 *
 * <h2>接入步骤（两步完成向量化）</h2>
 * <ol>
 *   <li><b>实体标注</b>：在实体类上加 {@code @VectorIndexed}，声明要向量化的字段和 Milvus Collection 名称
 *       （参考 {@link User}）</li>
 *   <li><b>注册实体</b>：实现本接口，在 {@link #entities()} 中返回所有需要向量同步的实体类</li>
 * </ol>
 *
 * <p>
 * 完成后，框架会自动进行 <b>增量同步</b>：定时将数据库中新增/变更的记录向量化并写入 Milvus，
 * Agent 在对话时即可通过语义检索找到相关业务数据（RAG 能力）。
 * </p>
 *
 * <h2>如何扩展</h2>
 * <p>如果业务中还有"产品"表也需要向量化，只需：</p>
 * <pre>{@code
 * @Override
 * public List<Class<?>> entities() {
 *     return List.of(User.class, Product.class);
 * }
 * }</pre>
 *
 * @see io.github.aiagent.vectorizer.annotation.VectorIndexed
 * @see io.github.aiagent.vectorizer.VectorSyncEntityProvider
 */
@Component
public class DemoVectorSyncEntityProvider implements VectorSyncEntityProvider {

    @Override
    public List<Class<?>> entities() {
        return List.of(User.class);
    }
}
