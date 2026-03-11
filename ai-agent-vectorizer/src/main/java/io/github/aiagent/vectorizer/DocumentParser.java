package io.github.aiagent.vectorizer;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析接口 —— 将非结构化文档转换为可向量化的文本片段。
 * <p>
 * 不同格式的文档（PDF、Word、Excel 等）由对应的实现类处理，
 * 将二进制文档流解析为文本片段列表，每个片段通常对应文档中的一个逻辑单元
 * （如一页、一个段落、一行数据）。
 * <p>
 * <b>当前状态：</b>此接口及实现为预留扩展能力，主同步链路（{@link VectorSyncService}）
 * 当前仅处理数据库表数据，尚未集成文档解析流程。
 *
 * @see io.github.aiagent.vectorizer.impl.PdfDocumentParser
 * @see io.github.aiagent.vectorizer.impl.WordDocumentParser
 * @see io.github.aiagent.vectorizer.impl.ExcelDocumentParser
 */
public interface DocumentParser {

    /**
     * 解析文档并提取文本片段。
     *
     * @param input    文档输入流，调用方负责关闭外层资源；实现类内部会读取并关闭此流
     * @param filename 原始文件名（含扩展名），可用于按后缀区分解析策略或记录日志
     * @return 文本片段列表，每个元素对应一个可独立向量化的文本单元；不可为 null
     * @throws IllegalStateException 解析失败时抛出，包含文件名信息
     */
    List<String> parse(InputStream input, String filename);
}
