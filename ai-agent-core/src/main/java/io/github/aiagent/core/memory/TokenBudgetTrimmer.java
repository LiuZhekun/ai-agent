package io.github.aiagent.core.memory;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import io.github.aiagent.core.model.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Token 预算裁剪器。
 */
@Component
public class TokenBudgetTrimmer {

    private final Encoding encoding;

    public TokenBudgetTrimmer() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncodingForModel(ModelType.GPT_4);
    }

    /**
     * 依据 Token 预算裁剪历史，始终保留 system 与最近 2 条消息。
     */
    public List<ChatMessage> trim(List<ChatMessage> history, int maxTokens) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> copy = new ArrayList<>(history);
        while (estimateTokens(copy) > maxTokens && copy.size() > 2) {
            int index = firstRemovableIndex(copy);
            if (index < 0) {
                break;
            }
            copy.remove(index);
        }
        return copy;
    }

    private int firstRemovableIndex(List<ChatMessage> history) {
        int keepStart = Math.max(history.size() - 2, 0);
        for (int i = 0; i < history.size(); i++) {
            if (i >= keepStart) {
                continue;
            }
            ChatMessage item = history.get(i);
            if (item.getRole() == ChatMessage.Role.SYSTEM) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private int estimateTokens(List<ChatMessage> history) {
        return history.stream()
                .map(ChatMessage::getContent)
                .mapToInt(text -> text == null ? 0 : encoding.countTokens(text))
                .sum();
    }
}
