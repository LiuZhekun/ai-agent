package io.github.aiagent.knowledge.snippet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * L0 知识片段加载器 —— 从 classpath 扫描并加载静态知识文件。
 * <p>
 * 默认扫描路径为 {@code classpath*:agent-knowledge/*.*}，可通过配置项
 * {@code ai.agent.knowledge.snippet.path} 自定义。支持 Spring 资源模式匹配，
 * 可加载 Markdown、文本等任意文本格式的知识文件。
 * <p>
 * 加载结果供 {@link io.github.aiagent.knowledge.KnowledgeManager} 组装 L0 层知识，
 * 也作为 RAG 索引的数据源（由 {@link io.github.aiagent.knowledge.rag.RagFullIndexer} 消费）。
 *
 * @see KnowledgeSnippet
 */
@Component
public class KnowledgeSnippetLoader {

    @Value("${ai.agent.knowledge.snippet.path:classpath*:agent-knowledge/*.*}")
    private String path;

    /**
     * 扫描配置路径下的所有资源文件，逐个读取为 {@link KnowledgeSnippet}。
     * <p>
     * 每个文件对应一个 Snippet，文件名作为 title，URI 作为 source。
     *
     * @return 知识片段列表；路径下无文件时返回空列表
     * @throws IllegalStateException 文件读取失败时抛出
     */
    public List<KnowledgeSnippet> load() {
        List<KnowledgeSnippet> result = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(path);
            for (Resource resource : resources) {
                KnowledgeSnippet snippet = new KnowledgeSnippet();
                snippet.setTitle(resource.getFilename());
                snippet.setSource(resource.getURI().toString());
                snippet.setContent(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                result.add(snippet);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Load knowledge snippets failed", ex);
        }
        return result;
    }
}
