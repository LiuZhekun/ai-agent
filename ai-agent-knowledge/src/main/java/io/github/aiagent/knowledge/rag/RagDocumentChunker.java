package io.github.aiagent.knowledge.rag;

import io.github.aiagent.knowledge.snippet.KnowledgeSnippet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档切分器 —— 将知识片段按固定窗口 + 重叠策略切分为可检索的 chunk。
 * <p>
 * 切分参数通过 {@link RagProperties} 配置：
 * <ul>
 *   <li>{@code chunkSize} —— 每个 chunk 的最大字符数（下限 128）</li>
 *   <li>{@code chunkOverlap} —— 相邻 chunk 之间的重叠字符数（上限为 chunkSize/2）</li>
 * </ul>
 * <p>
 * 重叠设计保证跨 chunk 边界的语义不会被完全截断，提升检索召回率。
 *
 * @see RagFullIndexer
 * @see RagDocumentChunk
 */
@Component
public class RagDocumentChunker {

    private final RagProperties ragProperties;

    public RagDocumentChunker(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * 将一个知识片段切分为多个 chunk。
     * <p>
     * 每个 chunk 会被分配唯一 ID，并继承原始片段的 title 和 source 信息。
     *
     * @param snippet 待切分的知识片段
     * @return chunk 列表；片段为空时返回空列表
     */
    public List<RagDocumentChunk> chunk(KnowledgeSnippet snippet) {
        List<RagDocumentChunk> chunks = new ArrayList<>();
        if (snippet == null || snippet.getContent() == null || snippet.getContent().isBlank()) {
            return chunks;
        }
        String normalized = snippet.getContent().replace("\r\n", "\n").trim();
        int chunkSize = Math.max(ragProperties.getChunkSize(), 128);
        int overlap = Math.max(Math.min(ragProperties.getChunkOverlap(), chunkSize / 2), 0);
        int step = Math.max(chunkSize - overlap, 32);

        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + chunkSize, normalized.length());
            String piece = normalized.substring(start, end).trim();
            if (piece.isBlank()) {
                continue;
            }
            RagDocumentChunk chunk = new RagDocumentChunk();
            chunk.setChunkId(UUID.randomUUID().toString());
            chunk.setTitle(snippet.getTitle());
            chunk.setSource(snippet.getSource());
            chunk.setContent(piece);
            chunk.setScore(0D);
            chunks.add(chunk);
            if (end >= normalized.length()) {
                break;
            }
        }
        return chunks;
    }
}
