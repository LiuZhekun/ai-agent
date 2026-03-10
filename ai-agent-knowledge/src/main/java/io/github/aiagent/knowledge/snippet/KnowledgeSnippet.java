package io.github.aiagent.knowledge.snippet;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识片段。
 */
public class KnowledgeSnippet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String title;
    private String content;
    private String source;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
