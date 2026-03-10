package io.github.aiagent.knowledge.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * RAG 检索器，当前使用内存索引实现通用能力。
 */
@Component
public class RagRetriever {

    private final AtomicReference<List<RagDocumentChunk>> indexRef = new AtomicReference<>(List.of());

    public void replaceIndex(List<RagDocumentChunk> chunks) {
        indexRef.set(chunks == null ? List.of() : List.copyOf(chunks));
    }

    public List<RagDocumentChunk> retrieve(String query, int topK, double minScore) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<RagDocumentChunk> source = indexRef.get();
        if (source.isEmpty()) {
            return List.of();
        }
        Set<String> queryTerms = splitTerms(query);
        if (queryTerms.isEmpty()) {
            return List.of();
        }
        List<RagDocumentChunk> scored = new ArrayList<>();
        for (RagDocumentChunk chunk : source) {
            double score = score(queryTerms, chunk.getContent());
            if (score < minScore) {
                continue;
            }
            RagDocumentChunk copy = new RagDocumentChunk();
            copy.setChunkId(chunk.getChunkId());
            copy.setTitle(chunk.getTitle());
            copy.setSource(chunk.getSource());
            copy.setContent(chunk.getContent());
            copy.setScore(score);
            scored.add(copy);
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(RagDocumentChunk::getScore).reversed())
                .limit(Math.max(topK, 1))
                .toList();
    }

    private double score(Set<String> queryTerms, String content) {
        if (content == null || content.isBlank()) {
            return 0D;
        }
        Set<String> contentTerms = splitTerms(content);
        if (contentTerms.isEmpty()) {
            return 0D;
        }
        long hit = queryTerms.stream().filter(contentTerms::contains).count();
        return (double) hit / (double) queryTerms.size();
    }

    private Set<String> splitTerms(String text) {
        return java.util.Arrays.stream(text.toLowerCase().split("[^a-z0-9\\u4e00-\\u9fa5]+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
