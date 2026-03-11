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
 * 基于 Token 预算的对话历史裁剪器。
 * <p>
 * 核心思路：给定一个 Token 上限（预算），从对话历史中按优先级移除消息，
 * 直到总 Token 数不超过预算。裁剪时遵循以下保护规则：
 * <ul>
 *   <li>SYSTEM 角色的消息永不移除（通常包含关键系统指令）</li>
 *   <li>最近 2 条消息永不移除（保证当前轮对话上下文连贯）</li>
 *   <li>优先移除最早的非 SYSTEM 消息</li>
 * </ul>
 * <p>
 * Token 计数使用 <a href="https://github.com/knuddelsgmbh/jtokkit">jtokkit</a> 库，
 * 以 GPT-4 的 Encoding 为基准进行精确计算。与 {@link ConversationMemoryRedisImpl#estimateTokens(String)}
 * 和 {@link SummaryCompressor} 中使用的 {@code length/2} 粗略估算不同，本类追求精确性，
 * 适用于 Token 预算策略下对裁剪精度有要求的场景。
 *
 * @see MemoryConfig.Strategy#TOKEN_WINDOW 对应的配置策略
 */
@Component
public class TokenBudgetTrimmer {

    private final Encoding encoding;

    public TokenBudgetTrimmer() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncodingForModel(ModelType.GPT_4);
    }

    /**
     * 在 Token 预算内裁剪对话历史。
     * <p>
     * 从前往后逐条移除可移除的消息，直到总 Token 数不超过 {@code maxTokens}
     * 或剩余消息已全部受保护无法移除。
     *
     * @param history   原始对话历史列表
     * @param maxTokens Token 预算上限
     * @return 裁剪后的消息列表（新列表，不修改原列表）；输入为空时返回空列表
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
