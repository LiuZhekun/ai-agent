package io.github.aiagent.core.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.aiagent.core.model.ChatMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Redis List 的 {@link ConversationMemory} 实现。
 * <p>
 * 存储策略：
 * <ul>
 *   <li>每个会话对应一个 Redis List，Key 格式为 {@code agent:memory:{sessionId}}</li>
 *   <li>每条消息序列化为 JSON 字符串后 RPUSH 到列表尾部，保持时间升序</li>
 *   <li>设置 30 分钟 TTL 自动过期，避免长时间未活跃的会话占用内存</li>
 * </ul>
 * <p>
 * 选择 Redis 作为默认存储的原因：对话历史属于临时性热数据，读写频繁但不需要持久化，
 * Redis 的高吞吐和自动过期机制非常契合此场景。如果需要持久化对话历史（如审计需求），
 * 可另行实现 {@link ConversationMemory} 接口。
 *
 * @see ConversationMemory 存储接口定义
 */
@Component
public class ConversationMemoryRedisImpl implements ConversationMemory {

    private static final String KEY_PREFIX = "agent:memory:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ConversationMemoryRedisImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 每次写入后刷新 TTL，确保活跃会话不会意外过期。
     */
    @Override
    public void save(String sessionId, ChatMessage message) {
        String key = KEY_PREFIX + sessionId;
        try {
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            redisTemplate.expire(key, TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialize chat message failed", ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ChatMessage> load(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChatMessage> result = new ArrayList<>(rawList.size());
        for (String item : rawList) {
            try {
                result.add(objectMapper.readValue(item, ChatMessage.class));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Deserialize chat message failed", ex);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void clear(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code content.length() / 2} 粗略估算 Token 数量。
     * 这是一种性能优先的折中：中文约 1 字 ≈ 1~2 Token，英文约 4~5 字符 ≈ 1 Token，
     * 取 length/2 作为中间值在混合语言场景下偏差可接受。
     * 如需精确计算，应使用 {@link TokenBudgetTrimmer} 中基于 jtokkit 的实现。
     */
    @Override
    public int estimateTokens(String sessionId) {
        return load(sessionId).stream().map(ChatMessage::getContent).mapToInt(s -> s == null ? 0 : s.length() / 2).sum();
    }
}
