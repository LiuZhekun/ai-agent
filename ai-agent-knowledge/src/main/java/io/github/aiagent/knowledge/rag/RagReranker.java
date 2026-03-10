package io.github.aiagent.knowledge.rag;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * RAG 重排器，使用轻量启发式策略对候选片段排序。
 */
@Component
public class RagReranker {

    public List<RagDocumentChunk> rerank(String query, List<RagDocumentChunk> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = query == null ? "" : query.trim();
        return candidates.stream()
                .sorted(Comparator
                        .comparingInt((RagDocumentChunk c) -> containsPhrase(c.getContent(), normalizedQuery) ? 1 : 0)
                        .reversed()
                        .thenComparing(Comparator.comparingDouble(RagDocumentChunk::getScore).reversed()))
                .limit(Math.max(topK, 1))
                .toList();
    }

    private boolean containsPhrase(String content, String query) {
        return !query.isBlank() && content != null && content.contains(query);
    }
}
