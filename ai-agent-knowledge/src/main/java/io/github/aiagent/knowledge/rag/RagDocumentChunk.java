package io.github.aiagent.knowledge.rag;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 文档 chunk —— 经 {@link RagDocumentChunker} 切分后的最小检索单元。
 * <p>
 * 每个 chunk 携带唯一 ID、来源信息和检索得分，
 * 在 {@link RagRetriever} 中作为索引条目存储，在 {@link RagContextAssembler} 中
 * 被组装为 Prompt 上下文。
 *
 * @see RagDocumentChunker
 * @see RagRetriever
 */
public class RagDocumentChunk implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String chunkId;
    private String title;
    private String source;
    private String content;
    private double score;

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
