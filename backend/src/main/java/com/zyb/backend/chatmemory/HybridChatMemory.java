package com.zyb.backend.chatmemory;

import com.zyb.backend.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 混合存储ChatMemory
 * AI原生应用的对话记忆混合存储方案
 * 支持 Redis + 文件双写，提供故障恢复能力
 */
@Slf4j
@Component
public class HybridChatMemory implements ChatMemory {

    private final RedisChatMemory redisChatMemory;
    private final FileBasedChatMemory fileBasedChatMemory;

    @Value("${voiceKeeper.agent.memory.storage-type:redis}")
    private String storageType;

    public HybridChatMemory(RedisChatMemory redisChatMemory) {
        this.redisChatMemory = redisChatMemory;
        this.fileBasedChatMemory = new FileBasedChatMemory(FileConstant.CHAT_MEMORY_DIR);
    }

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        switch (storageType.toLowerCase()) {
            case "redis":
                redisChatMemory.add(conversationId, messages);
                break;
            case "file":
                fileBasedChatMemory.add(conversationId, messages);
                break;
            case "hybrid":
            default:
                // 双写模式：同时写入Redis和文件
                try {
                    redisChatMemory.add(conversationId, messages);
                } catch (Exception e) {
                    log.error("⚠️ Redis写入失败，使用文件存储: {}", e.getMessage());
                }

                try {
                    fileBasedChatMemory.add(conversationId, messages);
                } catch (Exception e) {
                    log.error("⚠️ 文件写入失败: {}", e.getMessage());
                }
                break;
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        switch (storageType.toLowerCase()) {
            case "redis":
                return getFromRedis(conversationId);
            case "file":
                return fileBasedChatMemory.get(conversationId);
            case "hybrid":
            default:
                return getFromHybrid(conversationId);
        }
    }

    @Override
    public void clear(String conversationId) {
        switch (storageType.toLowerCase()) {
            case "redis":
                redisChatMemory.clear(conversationId);
                break;
            case "file":
                fileBasedChatMemory.clear(conversationId);
                break;
            case "hybrid":
            default:
                try {
                    redisChatMemory.clear(conversationId);
                } catch (Exception e) {
                    log.error("⚠️ Redis清空失败: {}", e.getMessage());
                }

                try {
                    fileBasedChatMemory.clear(conversationId);
                } catch (Exception e) {
                    log.error("⚠️ 文件清空失败: {}", e.getMessage());
                }
                break;
        }
    }

    /**
     * 从Redis获取数据，失败时回退到文件
     */
    private List<Message> getFromRedis(String conversationId) {
        try {
            List<Message> messages = redisChatMemory.get(conversationId);
            if (messages.isEmpty()) {
                // Redis中没有数据，尝试从文件加载
                log.debug("Redis中无数据，尝试从文件加载: {}", conversationId);
                return fileBasedChatMemory.get(conversationId);
            }
            return messages;
        } catch (Exception e) {
            log.error("⚠️ Redis读取失败，回退到文件存储: {}", e.getMessage());
            return fileBasedChatMemory.get(conversationId);
        }
    }

    /**
     * 混合模式：优先Redis，失败时使用文件
     */
    private List<Message> getFromHybrid(String conversationId) {
        try {
            List<Message> messages = redisChatMemory.get(conversationId);
            if (!messages.isEmpty()) {
                return messages;
            }
        } catch (Exception e) {
            log.warn("⚠️ Redis读取失败: {}", e.getMessage());
        }

        // Redis失败或无数据，使用文件存储
        try {
            List<Message> fileMessages = fileBasedChatMemory.get(conversationId);
            if (!fileMessages.isEmpty()) {
                // 将文件数据同步到Redis
                log.debug("📤 将文件数据同步到Redis: {}", conversationId);
                redisChatMemory.add(conversationId, fileMessages);
            }
            return fileMessages;
        } catch (Exception e) {
            log.error("❌ 文件读取也失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 数据迁移：从文件迁移到Redis
     */
    public void migrateFromFileToRedis(String conversationId) {
        try {
            List<Message> fileMessages = fileBasedChatMemory.get(conversationId);
            if (!fileMessages.isEmpty()) {
                redisChatMemory.add(conversationId, fileMessages);
                log.info("✅ 成功迁移会话数据到Redis: {}, 消息数: {}",
                        conversationId, fileMessages.size());
            }
        } catch (Exception e) {
            log.error("❌ 数据迁移失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取消息数量
     */
    public Long getMessageCount(String conversationId) {
        if ("redis".equals(storageType)) {
            return redisChatMemory.getMessageCount(conversationId);
        }
        return (long) get(conversationId).size();
    }

    /**
     * 检查会话是否存在
     */
    public boolean exists(String conversationId) {
        if ("redis".equals(storageType)) {
            return redisChatMemory.exists(conversationId);
        }
        return !get(conversationId).isEmpty();
    }
}

