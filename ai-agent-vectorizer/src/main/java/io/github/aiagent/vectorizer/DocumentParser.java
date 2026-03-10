package io.github.aiagent.vectorizer;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析接口。
 * 不同实现负责把二进制文档转成可向量化的文本片段列表。
 */
public interface DocumentParser {
    /**
     * @param input 文档输入流
     * @param filename 原始文件名，用于按后缀或规则区分解析策略
     * @return 文本片段列表（每个元素通常对应一个段落/页/行）
     */
    List<String> parse(InputStream input, String filename);
}
