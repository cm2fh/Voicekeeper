package com.zyb.backend.agent.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zyb.backend.agent.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 智能体管理服务
 * AI原生应用的核心：管理Agent实例的创建、缓存和生命周期
 */
@Slf4j
@Service
public class AgentManagerService {

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private ToolCallback[] voiceTools;

    @Resource
    private ChatModel chatModel;

    // 智能体实例缓存 - 30分钟过期
    private final Cache<String, BaseAgent> agentCache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats()
            .build();

    /**
     * 获取或创建 VoiceKeeper 智能体
     * AI原生应用的核心：一个会话对应一个Agent实例
     */
    public BaseAgent getOrCreateVoiceKeeperAgent(String conversationId) {
        String cacheKey = "voiceKeeper:" + conversationId;

        return agentCache.get(cacheKey, key -> {
            log.info("🤖 创建新的 VoiceKeeper 智能体实例: {}", conversationId);
            
            // TODO: 这里将在后续创建 VoiceKeeperAgent 时实现
            // VoiceKeeperAgent agent = new VoiceKeeperAgent(voiceTools, chatModel);
            // agent.setConversationInfo(conversationId, chatMemory);
            // return agent;
            
            throw new UnsupportedOperationException("VoiceKeeperAgent 尚未实现");
        });
    }

    /**
     * 生成会话ID
     */
    public String generateConversationId(String userId) {
        String userPart = (userId != null && !userId.isEmpty()) ? userId : "anonymous";
        return "voice:" + userPart + ":" + System.currentTimeMillis();
    }

    /**
     * 清理智能体缓存
     */
    public void clearAgentCache(String conversationId) {
        agentCache.invalidate("voiceKeeper:" + conversationId);
        log.info("🗑️ 清理智能体缓存: {}", conversationId);
    }

    /**
     * 清理会话数据
     */
    public void clearConversation(String conversationId) {
        // 清理对话记忆
        chatMemory.clear(conversationId);
        // 清理缓存中的智能体实例
        clearAgentCache(conversationId);
        log.info("🗑️ 清理会话数据: {}", conversationId);
    }

    /**
     * 检查会话是否存在
     */
    public boolean conversationExists(String conversationId) {
        try {
            var messages = chatMemory.get(conversationId);
            return !messages.isEmpty();
        } catch (Exception e) {
            log.warn("⚠️ 检查会话存在性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取会话消息数量
     */
    public int getMessageCount(String conversationId) {
        try {
            var messages = chatMemory.get(conversationId);
            return messages.size();
        } catch (Exception e) {
            log.warn("⚠️ 获取消息数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        var stats = agentCache.stats();
        return String.format("Agent缓存统计 - 命中率: %.2f%%, 总请求: %d, 缓存大小: %d",
                stats.hitRate() * 100,
                stats.requestCount(),
                agentCache.estimatedSize());
    }
}

