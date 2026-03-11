package io.github.aiagent.knowledge.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * L3 RAG 配置属性 —— 控制检索增强生成（RAG）管线的各项参数。
 * <p>
 * 对应配置前缀为 {@code ai.agent.knowledge.rag}，各参数说明：
 * <ul>
 *   <li>{@code enabled} —— 是否启用 RAG 功能</li>
 *   <li>{@code chunkSize} / {@code chunkOverlap} —— 文档切分窗口大小和重叠量</li>
 *   <li>{@code topK} —— 初次检索返回的候选数量</li>
 *   <li>{@code rerankTopK} —— 重排后保留的片段数量</li>
 *   <li>{@code minScore} —— 检索最低得分阈值</li>
 *   <li>{@code maxContextChars} —— 注入 Prompt 的上下文最大字符数</li>
 * </ul>
 *
 * @see RagRetriever
 * @see RagDocumentChunker
 * @see RagReranker
 */
@Component
public class RagProperties {

    @Value("${ai.agent.knowledge.rag.enabled:true}")
    private boolean enabled;

    @Value("${ai.agent.knowledge.rag.chunk-size:400}")
    private int chunkSize;

    @Value("${ai.agent.knowledge.rag.chunk-overlap:80}")
    private int chunkOverlap;

    @Value("${ai.agent.knowledge.rag.top-k:8}")
    private int topK;

    @Value("${ai.agent.knowledge.rag.rerank-top-k:5}")
    private int rerankTopK;

    @Value("${ai.agent.knowledge.rag.min-score:0.05}")
    private double minScore;

    @Value("${ai.agent.knowledge.rag.max-context-chars:2200}")
    private int maxContextChars;

    public boolean isEnabled() {
        return enabled;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public int getTopK() {
        return topK;
    }

    public int getRerankTopK() {
        return rerankTopK;
    }

    public double getMinScore() {
        return minScore;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }
}
