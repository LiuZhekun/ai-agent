package io.github.aiagent.knowledge.rag;

import io.github.aiagent.knowledge.snippet.KnowledgeSnippet;
import io.github.aiagent.knowledge.snippet.KnowledgeSnippetLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 全量索引构建器 —— 负责从知识片段构建和刷新 {@link RagRetriever} 的内存索引。
 * <p>
 * 启动时（{@code @PostConstruct}）自动构建一次索引。
 * 运行期间可通过 {@link #rebuildAllIndex()} 手动触发全量重建，
 * 也会在 {@link io.github.aiagent.knowledge.KnowledgeManager} 检索为空时被自动调用。
 * <p>
 * 构建流程：加载知识片段 → 切分为检索粒度的 chunk → 替换检索器索引。
 *
 * @see RagDocumentChunker
 * @see RagRetriever
 */
@Component
public class RagFullIndexer {

    private final KnowledgeSnippetLoader snippetLoader;
    private final RagDocumentChunker chunker;
    private final RagRetriever retriever;
    private final RagProperties ragProperties;

    public RagFullIndexer(
            KnowledgeSnippetLoader snippetLoader,
            RagDocumentChunker chunker,
            RagRetriever retriever,
            RagProperties ragProperties) {
        this.snippetLoader = snippetLoader;
        this.chunker = chunker;
        this.retriever = retriever;
        this.ragProperties = ragProperties;
    }

    /**
     * 应用启动时自动构建索引（仅在 RAG 功能启用时执行）。
     */
    @PostConstruct
    public void init() {
        if (ragProperties.isEnabled()) {
            rebuildAllIndex();
        }
    }

    /**
     * 全量重建 RAG 内存索引。
     * <p>
     * synchronized 保证并发调用时只有一个线程执行重建，避免重复加载和切分。
     *
     * @return 构建后的 chunk 总数
     */
    public synchronized int rebuildAllIndex() {
        List<KnowledgeSnippet> snippets = snippetLoader.load();
        List<RagDocumentChunk> chunks = new ArrayList<>();
        for (KnowledgeSnippet snippet : snippets) {
            chunks.addAll(chunker.chunk(snippet));
        }
        retriever.replaceIndex(chunks);
        return chunks.size();
    }
}
