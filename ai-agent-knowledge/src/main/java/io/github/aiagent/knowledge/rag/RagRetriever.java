package io.github.aiagent.knowledge.rag;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    private final ApplicationContext applicationContext;
    private final AtomicReference<List<RagDocumentChunk>> indexRef = new AtomicReference<>(List.of());

    public RagRetriever(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public String backendName() {
        Object vectorStore = getVectorStoreBean();
        if (vectorStore != null) {
            return vectorStore.getClass().getSimpleName();
        }
        return "rag-in-memory-index";
    }

    public boolean isVectorStoreAvailable() {
        return getVectorStoreBean() != null;
    }

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
        Object vectorStore = getVectorStoreBean();
        if (vectorStore != null) {
            return retrieveFromVectorStore(vectorStore, query, topK, minScore);
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

    @SuppressWarnings("unchecked")
    private List<RagDocumentChunk> retrieveFromVectorStore(Object vectorStore, String query, int topK, double minScore) {
        List<?> docs = invokeSimilaritySearch(vectorStore, query, topK, minScore);
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        List<RagDocumentChunk> chunks = new ArrayList<>(docs.size());
        for (Object doc : docs) {
            String content = firstNonBlank(invokeString(doc, "getText"), invokeString(doc, "getFormattedContent"));
            if (content == null || content.isBlank()) {
                continue;
            }
            Map<String, Object> metadata = invokeMetadata(doc);
            RagDocumentChunk chunk = new RagDocumentChunk();
            chunk.setChunkId(firstNonBlank(invokeString(doc, "getId"), metadataValue(metadata, "rowId"), metadataValue(metadata, "id")));
            chunk.setTitle(firstNonBlank(metadataValue(metadata, "title"), metadataValue(metadata, "table"), "vector-doc"));
            chunk.setSource(firstNonBlank(metadataValue(metadata, "source"), metadataValue(metadata, "collection"), metadataValue(metadata, "entityClass")));
            chunk.setContent(content);
            chunk.setScore(extractScore(doc, metadata));
            chunks.add(chunk);
        }
        return chunks;
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeSimilaritySearch(Object vectorStore, String query, int topK, double minScore) {
        try {
            Class<?> requestClass = Class.forName("org.springframework.ai.vectorstore.SearchRequest");
            Method builderMethod = requestClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            invokeBuilderIfExists(builder, "query", query);
            invokeBuilderIfExists(builder, "topK", Math.max(topK, 1));
            invokeBuilderIfExists(builder, "similarityThreshold", Math.max(minScore, 0D));
            Object request = builder.getClass().getMethod("build").invoke(builder);
            Method search = vectorStore.getClass().getMethod("similaritySearch", requestClass);
            Object result = search.invoke(vectorStore, request);
            if (result instanceof List<?> list) {
                return list;
            }
        } catch (Exception ignored) {
            // 兼容不含 SearchRequest 类型的 Spring AI 版本，降级为字符串查询
        }

        try {
            Method search = vectorStore.getClass().getMethod("similaritySearch", String.class);
            Object result = search.invoke(vectorStore, query);
            if (result instanceof List<?> list) {
                return list;
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Object getVectorStoreBean() {
        try {
            Class<?> vectorStoreClass = Class.forName("org.springframework.ai.vectorstore.VectorStore");
            return applicationContext.getBeanProvider((Class<Object>) vectorStoreClass).getIfAvailable();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void invokeBuilderIfExists(Object target, String method, Object arg) {
        try {
            for (Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(method) || m.getParameterCount() != 1) {
                    continue;
                }
                Class<?> paramType = m.getParameterTypes()[0];
                if (arg == null || wrapPrimitive(paramType).isAssignableFrom(arg.getClass())) {
                    m.invoke(target, arg);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeMetadata(Object doc) {
        try {
            Object metadata = doc.getClass().getMethod("getMetadata").invoke(doc);
            if (metadata instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }

    private double extractScore(Object doc, Map<String, Object> metadata) {
        try {
            Object score = doc.getClass().getMethod("getScore").invoke(doc);
            if (score instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception ignored) {
        }
        Object metadataScore = metadata.get("score");
        if (metadataScore instanceof Number number) {
            return number.doubleValue();
        }
        return 0D;
    }

    private String invokeString(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String metadataValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
