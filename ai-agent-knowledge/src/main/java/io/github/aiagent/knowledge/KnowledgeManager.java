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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识管理器 —— 按分层策略（L0-L3）汇总并组装可注入 Prompt 的知识上下文。
 * <p>
 * 每一层都有独立的 {@code enabled} 开关，业务项目可按需精确控制：
 * <ul>
 *   <li><b>L0 静态片段</b>（{@code ai.agent.knowledge.snippet.enabled}）—
 *       通过 {@link KnowledgeSnippetLoader} 从 classpath 加载的业务知识文件</li>
 *   <li><b>L1 数据库 Schema</b>（{@code ai.agent.knowledge.schema.enabled}）—
 *       通过 {@link SchemaDiscoveryService} 自动读取的表结构元数据</li>
 *   <li><b>L2 安全 SQL</b>（{@code ai.agent.knowledge.sql.enabled}）—
 *       由 {@link io.github.aiagent.knowledge.sql.SafeSqlQueryTool} 提供的运行时查询能力</li>
 *   <li><b>L3 RAG</b>（{@code ai.agent.knowledge.rag.enabled}）—
 *       根据用户查询动态检索的向量化知识</li>
 * </ul>
 *
 * @see KnowledgeLevel
 * @see KnowledgeAdvisor
 */
@Component
public class KnowledgeManager {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeManager.class);

    @Value("${ai.agent.knowledge.snippet.enabled:true}")
    private boolean snippetEnabled;

    @Value("${ai.agent.knowledge.schema.enabled:true}")
    private boolean schemaEnabled;

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

    /**
     * 无查询上下文时的知识组装入口，仅包含 L0 + L1 静态知识。
     *
     * @return 组装后的知识 Prompt 文本
     */
    public String getKnowledgePrompt() {
        return getKnowledgePrompt("");
    }

    /**
     * 根据用户查询动态组装完整的知识 Prompt（L0 片段 + L1 Schema + L3 RAG 上下文）。
     * <p>
     * 当 RAG 首次检索为空时会自动触发一次全量索引重建后再次检索，
     * 兼顾冷启动场景。
     *
     * @param query 用户当前的自然语言查询，用于 L3 RAG 检索；为空时跳过 RAG
     * @return 组装后的知识 Prompt 文本
     */
    public String getKnowledgePrompt(String query) {
        StringBuilder sb = new StringBuilder();

        if (snippetEnabled) {
            String snippets = snippetLoader.load().stream()
                    .map(s -> "# " + s.getTitle() + "\n" + s.getContent())
                    .reduce("", (a, b) -> a + "\n" + b);
            sb.append(snippets);
        } else {
            log.debug("L0 知识片段已禁用 (ai.agent.knowledge.snippet.enabled=false)");
        }

        if (schemaEnabled) {
            String schema = schemaPromptGenerator.generate(schemaDiscoveryService.getCachedOrLoad());
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append(schema);
        } else {
            log.debug("L1 Schema 发现已禁用 (ai.agent.knowledge.schema.enabled=false)");
        }

        String ragContext = buildRagContext(query);
        if (!ragContext.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append(ragContext);
        }

        return sb.toString();
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
