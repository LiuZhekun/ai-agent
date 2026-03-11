package io.github.aiagent.knowledge.rag;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * RAG 重排器 —— 对初次检索的候选片段进行二次排序以提升相关性。
 * <p>
 * 使用轻量级启发式策略：优先提升包含完整查询短语的片段，
 * 其次按原始检索得分排序。最终截取 top-K 返回。
 * <p>
 * <b>设计取舍：</b>当前为规则重排，零额外延迟。
 * 如需更高精度可替换为 Cross-Encoder 模型重排。
 *
 * @see RagRetriever
 * @see RagContextAssembler
 */
@Component
public class RagReranker {

    /**
     * 对候选片段执行启发式重排。
     * <p>
     * 排序规则：包含完整查询短语的片段排在前面，相同条件下按检索得分降序。
     *
     * @param query      用户查询文本
     * @param candidates 初次检索返回的候选片段
     * @param topK       重排后保留的最大片段数
     * @return 重排后的片段列表
     */
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
