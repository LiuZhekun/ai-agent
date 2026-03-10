package io.github.aiagent.core.memory;

import java.util.List;

/**
 * 向量知识库抽象，由 vectorizer 模块提供实现。
 */
public interface VectorKnowledgeBase {

    List<String> search(String query, int topK, double threshold);
}
