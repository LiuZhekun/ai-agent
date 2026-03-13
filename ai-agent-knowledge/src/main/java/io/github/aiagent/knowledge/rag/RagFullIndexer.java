package io.github.aiagent.knowledge.rag;

import io.github.aiagent.knowledge.snippet.KnowledgeSnippet;
import io.github.aiagent.knowledge.snippet.KnowledgeSnippetLoader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
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

    private static final Logger log = LoggerFactory.getLogger(RagFullIndexer.class);

    private final KnowledgeSnippetLoader snippetLoader;
    private final RagDocumentChunker chunker;
    private final RagRetriever retriever;
    private final RagProperties ragProperties;
    private final ApplicationContext applicationContext;

    public RagFullIndexer(
            KnowledgeSnippetLoader snippetLoader,
            RagDocumentChunker chunker,
            RagRetriever retriever,
            RagProperties ragProperties,
            ApplicationContext applicationContext) {
        this.snippetLoader = snippetLoader;
        this.chunker = chunker;
        this.retriever = retriever;
        this.ragProperties = ragProperties;
        this.applicationContext = applicationContext;
    }

    /**
     * 应用启动时自动构建索引（仅在 RAG 功能启用时执行）。
     */
    @PostConstruct
    public void init() {
        if (ragProperties.isEnabled()) {
            if (!retriever.isVectorStoreAvailable()) {
                rebuildAllIndex();
            } else {
                log.info("RAG 使用 VectorStore 检索，跳过内存索引初始化: backend={}", retriever.backendName());
            }
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
        if (retriever.isVectorStoreAvailable()) {
            triggerVectorSync();
            return 0;
        }
        List<KnowledgeSnippet> snippets = snippetLoader.load();
        List<RagDocumentChunk> chunks = new ArrayList<>();
        for (KnowledgeSnippet snippet : snippets) {
            chunks.addAll(chunker.chunk(snippet));
        }
        retriever.replaceIndex(chunks);
        return chunks.size();
    }

    private void triggerVectorSync() {
        try {
            Class<?> schedulerClass = Class.forName("io.github.aiagent.vectorizer.VectorSyncScheduler");
            Object scheduler = applicationContext.getBean(schedulerClass);
            schedulerClass.getMethod("sync").invoke(scheduler);
            log.info("已触发向量同步用于 RAG 重建: scheduler=VectorSyncScheduler");
        } catch (ClassNotFoundException ex) {
            log.warn("Classpath 中未找到 VectorSyncScheduler，无法触发向量重建");
        } catch (Exception ex) {
            log.warn("触发 VectorSyncScheduler 失败: {}", ex.getMessage(), ex);
        }
    }
}
