package io.github.aiagent.knowledge.snippet;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识片段数据模型 —— 表示从 classpath 加载的一个静态知识文件。
 * <p>
 * 由 {@link KnowledgeSnippetLoader} 创建，包含文件标题（文件名）、来源 URI 和文本内容。
 * 在知识组装流程中作为 L0 层知识的载体，同时也是 RAG 切分和索引的输入单元。
 *
 * @see KnowledgeSnippetLoader
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
