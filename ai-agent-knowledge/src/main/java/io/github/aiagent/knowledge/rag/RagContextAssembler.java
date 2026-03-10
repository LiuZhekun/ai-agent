package io.github.aiagent.knowledge.rag;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 上下文组装器，将片段组装为可直接注入 Prompt 的上下文。
 */
@Component
public class RagContextAssembler {

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
