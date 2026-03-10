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
 * Redis 对话记忆实现。
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

    @Override
    public void clear(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

    @Override
    public int estimateTokens(String sessionId) {
        return load(sessionId).stream().map(ChatMessage::getContent).mapToInt(s -> s == null ? 0 : s.length() / 2).sum();
    }
}
