package com.zyb.backend.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis版本的ChatMemory实现
 */
@Slf4j
@Component
public class RedisChatMemory implements ChatMemory {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String keyPrefix = "voiceKeeper:chat:";
    private final Duration expireTime = Duration.ofDays(30);

    public RedisChatMemory(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = keyPrefix + conversationId;

        try {
            // 使用RedisTemplate的序列化机制
            for (Message message : messages) {
                if (message != null) {
                    redisTemplate.opsForList().rightPush(key, message);
                }
            }

            // 设置过期时间
            redisTemplate.expire(key, expireTime);

            // 限制消息数量（保留最近1000条）
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > 1000) {
                redisTemplate.opsForList().trim(key, -1000, -1);
            }

            log.debug("成功添加 {} 条消息到会话: {}", messages.size(), conversationId);

        } catch (Exception e) {
            log.error("保存消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存消息失败", e);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        String key = keyPrefix + conversationId;
        List<Message> messages = new ArrayList<>();

        try {
            List<Object> messageObjects = redisTemplate.opsForList().range(key, 0, -1);

            if (messageObjects != null) {
                for (Object obj : messageObjects) {
                    if (obj instanceof Message) {
                        messages.add((Message) obj);
                    }
                }
            }

            log.debug("从会话 {} 获取到 {} 条消息", conversationId, messages.size());

        } catch (SerializationException e) {
            log.warn("会话 {} 的数据反序列化失败，自动清理: {}", conversationId, e.getMessage());
            clearCorruptedData(conversationId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取消息失败: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("deserialize")) {
                log.warn("检测到序列化问题，清理会话 {} 的数据", conversationId);
                clearCorruptedData(conversationId);
                return new ArrayList<>();
            }
        }

        return messages;
    }

    @Override
    public void clear(String conversationId) {
        String key = keyPrefix + conversationId;
        redisTemplate.delete(key);
        log.debug("清空会话: {}", conversationId);
    }

    /**
     * 获取会话消息数量
     */
    public Long getMessageCount(String conversationId) {
        String key = keyPrefix + conversationId;
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 清理损坏的数据
     */
    private void clearCorruptedData(String conversationId) {
        String key = keyPrefix + conversationId;
        try {
            redisTemplate.delete(key);
            log.info("已清理会话 {} 的损坏数据", conversationId);
        } catch (Exception e) {
            log.error("清理损坏数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查会话是否存在
     */
    public boolean exists(String conversationId) {
        String key = keyPrefix + conversationId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

