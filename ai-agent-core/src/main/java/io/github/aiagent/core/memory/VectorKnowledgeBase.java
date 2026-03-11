package io.github.aiagent.core.memory;

import java.util.List;

/**
 * 向量知识库检索抽象接口，为 Agent 提供长期记忆/知识检索能力。
 * <p>
 * 本接口定义了基于语义相似度的知识检索契约，由 {@code ai-agent-vectorizer} 模块
 * 或业务模块提供具体实现（如 Milvus、Elasticsearch KNN 等）。
 * {@link AgentMemoryManager#searchKnowledge(String)} 通过此接口实现长期记忆检索。
 * <p>
 * 本接口被声明为可选依赖（{@code @Nullable}），当 Spring 容器中不存在实现 Bean 时，
 * {@code AgentMemoryManager} 会优雅降级为返回空列表，不影响正常对话流程。
 * 这种设计使得向量能力成为一个可插拔的增强模块。
 * <p>
 * 实现示例：
 * <pre>{@code
 * @Component
 * public class MilvusVectorKnowledgeBase implements VectorKnowledgeBase {
 *     private final VectorStore vectorStore;
 *
 *     @Override
 *     public List<String> search(String query, int topK, double threshold) {
 *         SearchRequest request = SearchRequest.builder()
 *             .query(query).topK(topK).similarityThreshold(threshold).build();
 *         return vectorStore.similaritySearch(request)
 *             .stream().map(Document::getText).toList();
 *     }
 * }
 * }</pre>
 *
 * @see AgentMemoryManager 记忆管理器（本接口的主要消费方）
 * @see MemoryConfig#getVectorTopK() 向量检索 Top-K 配置
 * @see MemoryConfig#getVectorThreshold() 向量检索相似度阈值配置
 */
public interface VectorKnowledgeBase {

    /**
     * 在向量知识库中按语义相似度检索。
     *
     * @param query     用户查询文本，将被向量化后与知识库中的向量做相似度比较
     * @param topK      最多返回的结果条数
     * @param threshold 相似度阈值（0~1），低于此值的结果将被过滤
     * @return 匹配的文本片段列表，按相似度降序排列；无匹配时返回空列表
     */
    List<String> search(String query, int topK, double threshold);
}
