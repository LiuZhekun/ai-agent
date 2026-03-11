package io.github.aiagent.knowledge.rag;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 上下文组装器 —— 将重排后的文档片段组装为带来源引用的 Prompt 文本。
 * <p>
 * 每个片段按 {@code [序号] 标题 | source=来源 | score=得分} 格式输出，
 * 便于模型在回答时引用知识来源。组装过程中动态检查字符总数，
 * 超过 {@code maxChars} 限制时截断，确保不超出 token 预算。
 *
 * @see RagReranker
 * @see io.github.aiagent.knowledge.KnowledgeManager
 */
@Component
public class RagContextAssembler {

    /**
     * 将文档片段列表组装为可注入 Prompt 的引用上下文文本。
     *
     * @param chunks   经过重排的文档片段列表
     * @param maxChars 输出文本的最大字符数（下限 600）
     * @return 带来源引用的上下文文本；入参为空时返回空字符串
     */
    public String assemble(List<RagDocumentChunk> chunks, int maxChars) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        int limit = Math.max(maxChars, 600);
        StringBuilder builder = new StringBuilder();
        builder.append("以下为基于用户问题检索出的参考上下文（带来源引用）：\n");
        int i = 1;
        for (RagDocumentChunk chunk : chunks) {
            String row = "[" + i + "] "
                    + safe(chunk.getTitle())
                    + " | source=" + safe(chunk.getSource())
                    + " | score=" + String.format("%.3f", chunk.getScore())
                    + "\n" + safe(chunk.getContent()).trim() + "\n\n";
            if (builder.length() + row.length() > limit) {
                break;
            }
            builder.append(row);
            i++;
        }
        return builder.toString().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
