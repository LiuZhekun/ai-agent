package io.github.aiagent.core.memory;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import io.github.aiagent.core.model.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 LLM 的对话历史摘要压缩器。
 * <p>
 * 当对话历史的 Token 数超过阈值时，将前半段消息通过 LLM 生成一条摘要，
 * 用摘要消息替换原始的前半段历史，从而在保留核心语义的同时大幅减少 Token 占用。
 * <p>
 * 适用场景：长对话且历史信息对后续推理仍有参考价值的场景。相比
 * {@link TokenBudgetTrimmer} 的硬裁剪（直接丢弃），摘要压缩能保留更多语义信息，
 * 但代价是额外的 LLM API 调用（增加延迟和成本）。
 * <p>
 * Token 计数与 {@link TokenBudgetTrimmer} 统一使用
 * <a href="https://github.com/knuddelsgmbh/jtokkit">jtokkit</a> 库精确计算。
 *
 * @see MemoryConfig.Strategy#SUMMARY 对应的配置策略
 * @see TokenBudgetTrimmer 基于精确 Token 计算的裁剪方案
 */
@Component
public class SummaryCompressor {

    private final ChatClient chatClient;
    private final Encoding encoding;

    public SummaryCompressor(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncodingForModel(ModelType.GPT_4);
    }

    /**
     * 当 Token 数超过阈值时，压缩前半段历史为一条 SYSTEM 角色的摘要消息。
     * <p>
     * 压缩逻辑：将对话历史一分为二，前半段交给 LLM 生成简明摘要，
     * 返回"摘要消息 + 后半段原始消息"的新列表。
     * 如果 Token 数未超阈值或消息数不足 4 条，则直接返回原列表不做压缩。
     *
     * @param history   原始对话历史列表
     * @param threshold Token 阈值，超过此值触发压缩
     * @return 压缩后的消息列表；未触发压缩时返回原列表；输入为空时返回空列表
     */
    public List<ChatMessage> compress(List<ChatMessage> history, int threshold) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int totalTokens = estimateTokens(history);
        if (totalTokens <= threshold || history.size() < 4) {
            return history;
        }

        int half = history.size() / 2;
        List<ChatMessage> firstHalf = history.subList(0, half);
        List<ChatMessage> secondHalf = history.subList(half, history.size());
        StringBuilder source = new StringBuilder();
        for (ChatMessage message : firstHalf) {
            source.append(message.getRole()).append(": ").append(message.getContent()).append('\n');
        }
        String summaryText = chatClient.prompt()
                .user("请将以下对话内容压缩为简明摘要：\n" + source)
                .call()
                .content();

        ChatMessage summary = new ChatMessage();
        summary.setRole(ChatMessage.Role.SYSTEM);
        summary.setContent(summaryText);
        summary.setTimestamp(Instant.now());

        List<ChatMessage> result = new ArrayList<>();
        result.add(summary);
        result.addAll(secondHalf);
        return result;
    }

    private int estimateTokens(List<ChatMessage> history) {
        return history.stream()
                .map(ChatMessage::getContent)
                .mapToInt(text -> text == null ? 0 : encoding.countTokens(text))
                .sum();
    }
}
