package io.github.aiagent.knowledge.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * RAG 内存检索器 —— 基于 BM25 风格的词项匹配在内存中实现轻量级文档检索。
 * <p>
 * <b>评分策略：</b>将查询和文档都按非字母数字字符分词后，计算查询词项在文档中的
 * 命中率（命中数 / 查询词项数）。支持中文字符作为词项。
 * <p>
 * <b>索引管理：</b>通过 {@link #replaceIndex(java.util.List)} 原子替换内存索引，
 * 由 {@link RagFullIndexer} 在启动和刷新时调用。检索操作线程安全，无锁读取。
 * <p>
 * <b>设计取舍：</b>当前为纯内存实现，适合中小规模知识库（万级片段以内）。
 * 大规模场景可替换为向量数据库实现。
 *
 * @see RagFullIndexer
 * @see RagDocumentChunk
 */
@Component
public class RagRetriever {

    private final AtomicReference<List<RagDocumentChunk>> indexRef = new AtomicReference<>(List.of());

    /**
     * 原子替换内存中的文档索引。
     *
     * @param chunks 新的文档片段列表；为 null 时清空索引
     */
    public void replaceIndex(List<RagDocumentChunk> chunks) {
        indexRef.set(chunks == null ? List.of() : List.copyOf(chunks));
    }

    /**
     * 根据查询检索最相关的文档片段。
     *
     * @param query    用户查询文本
     * @param topK     最多返回的片段数
     * @param minScore 最低得分阈值，低于此分的片段被过滤
     * @return 按得分降序排列的文档片段列表
     */
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
