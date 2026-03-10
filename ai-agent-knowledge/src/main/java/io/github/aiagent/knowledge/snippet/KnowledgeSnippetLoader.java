package io.github.aiagent.knowledge.snippet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * L0 知识片段加载器。
 */
@Component
public class KnowledgeSnippetLoader {

    @Value("${ai.agent.knowledge.snippet.path:classpath*:agent-knowledge/*.*}")
    private String path;

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
