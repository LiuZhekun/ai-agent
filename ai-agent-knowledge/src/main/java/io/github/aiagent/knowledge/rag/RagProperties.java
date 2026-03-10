package io.github.aiagent.knowledge.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * L3 RAG 配置。
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
