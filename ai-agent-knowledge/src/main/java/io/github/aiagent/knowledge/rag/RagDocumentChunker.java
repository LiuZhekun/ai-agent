package io.github.aiagent.knowledge.rag;

import io.github.aiagent.knowledge.snippet.KnowledgeSnippet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档切分器，将长文本切分为可检索片段。
 */
@Component
public class RagDocumentChunker {

    private final RagProperties ragProperties;

    public RagDocumentChunker(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

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
