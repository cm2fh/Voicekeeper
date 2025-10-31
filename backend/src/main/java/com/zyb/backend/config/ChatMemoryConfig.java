package com.zyb.backend.config;

import com.zyb.backend.chatmemory.HybridChatMemory;
import com.zyb.backend.chatmemory.SummarizingChatMemoryDecorator;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 对话记忆配置类
 */
@Configuration
public class ChatMemoryConfig {

    @Value("${voiceKeeper.agent.memory.summary.threshold}")
    private int summaryThreshold;

    @Value("${voiceKeeper.agent.memory.summary.chunk-size}")
    private int summaryChunkSize;

    /**
     * 创建并配置记忆摘要装饰器
     * 当对话超过阈值时，自动使用AI生成摘要压缩历史
     */
    @Bean
    @Primary
    public ChatMemory summarizingChatMemory(HybridChatMemory hybridChatMemory, ChatModel chatModel) {
        return new SummarizingChatMemoryDecorator(
                hybridChatMemory,
                chatModel,
                summaryThreshold,
                summaryChunkSize
        );
    }
}

