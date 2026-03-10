package io.github.aiagent.knowledge.rag;

import io.github.aiagent.knowledge.snippet.KnowledgeSnippet;
import io.github.aiagent.knowledge.snippet.KnowledgeSnippetLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 全量索引入口，负责构建和刷新检索索引。
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

    @PostConstruct
    public void init() {
        if (ragProperties.isEnabled()) {
            rebuildAllIndex();
        }
    }

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
