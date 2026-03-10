package io.github.aiagent.knowledge;

import io.github.aiagent.knowledge.schema.SchemaDiscoveryService;
import io.github.aiagent.knowledge.schema.SchemaPromptGenerator;
import io.github.aiagent.knowledge.snippet.KnowledgeSnippetLoader;
import io.github.aiagent.knowledge.rag.RagContextAssembler;
import io.github.aiagent.knowledge.rag.RagDocumentChunk;
import io.github.aiagent.knowledge.rag.RagFullIndexer;
import io.github.aiagent.knowledge.rag.RagProperties;
import io.github.aiagent.knowledge.rag.RagReranker;
import io.github.aiagent.knowledge.rag.RagRetriever;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识管理器，汇总可注入 Prompt 的知识文本。
 */
@Component
public class KnowledgeManager {

    private final KnowledgeSnippetLoader snippetLoader;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final SchemaPromptGenerator schemaPromptGenerator;
    private final RagProperties ragProperties;
    private final RagRetriever ragRetriever;
    private final RagReranker ragReranker;
    private final RagContextAssembler ragContextAssembler;
    private final RagFullIndexer ragFullIndexer;

    public KnowledgeManager(
            KnowledgeSnippetLoader snippetLoader,
            SchemaDiscoveryService schemaDiscoveryService,
            SchemaPromptGenerator schemaPromptGenerator,
            RagProperties ragProperties,
            RagRetriever ragRetriever,
            RagReranker ragReranker,
            RagContextAssembler ragContextAssembler,
            RagFullIndexer ragFullIndexer) {
        this.snippetLoader = snippetLoader;
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.schemaPromptGenerator = schemaPromptGenerator;
        this.ragProperties = ragProperties;
        this.ragRetriever = ragRetriever;
        this.ragReranker = ragReranker;
        this.ragContextAssembler = ragContextAssembler;
        this.ragFullIndexer = ragFullIndexer;
    }

    public String getKnowledgePrompt() {
        return getKnowledgePrompt("");
    }

    /**
     * 根据查询动态组装知识 Prompt，L3 会注入带来源引用的 RAG context。
     */
    public String getKnowledgePrompt(String query) {
        String snippets = snippetLoader.load().stream()
                .map(s -> "# " + s.getTitle() + "\n" + s.getContent())
                .reduce("", (a, b) -> a + "\n" + b);
        String schema = schemaPromptGenerator.generate(schemaDiscoveryService.getCachedOrLoad());
        String ragContext = buildRagContext(query);
        return snippets + "\n\n" + schema + (ragContext.isBlank() ? "" : "\n\n" + ragContext);
    }

    private String buildRagContext(String query) {
        if (!ragProperties.isEnabled() || query == null || query.isBlank()) {
            return "";
        }
        List<RagDocumentChunk> retrieved = ragRetriever.retrieve(
                query,
                ragProperties.getTopK(),
                ragProperties.getMinScore());
        if (retrieved.isEmpty()) {
            ragFullIndexer.rebuildAllIndex();
            retrieved = ragRetriever.retrieve(query, ragProperties.getTopK(), ragProperties.getMinScore());
        }
        if (retrieved.isEmpty()) {
            return "";
        }
        List<RagDocumentChunk> reranked = ragReranker.rerank(query, retrieved, ragProperties.getRerankTopK());
        String assembled = ragContextAssembler.assemble(reranked, ragProperties.getMaxContextChars());
        return assembled.isBlank() ? "" : "L3 RAG 上下文：\n" + assembled;
    }
}
