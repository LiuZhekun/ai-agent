package io.github.aiagent.core.memory;

import io.github.aiagent.core.model.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话摘要压缩器。
 */
@Component
public class SummaryCompressor {

    private final ChatClient chatClient;

    public SummaryCompressor(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 超过阈值时压缩前半段历史为一条系统摘要消息。
     */
    public List<ChatMessage> compress(List<ChatMessage> history, int threshold) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int roughTokens = history.stream().map(ChatMessage::getContent).mapToInt(s -> s == null ? 0 : s.length() / 2).sum();
        if (roughTokens <= threshold || history.size() < 4) {
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
}
