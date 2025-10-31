package com.zyb.backend.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 记忆摘要装饰器
 * AI原生应用的智能记忆管理：当对话过长时，自动使用AI生成摘要
 * 
 * 工作原理：
 * 1. 监控对话长度，超过阈值触发摘要
 * 2. 使用AI模型生成历史对话的摘要
 * 3. 保留摘要 + 最近的消息，压缩记忆
 * 4. 异步执行，不阻塞用户请求
 */
@Slf4j
public class SummarizingChatMemoryDecorator implements ChatMemory {

    /**
     * 被装饰的 ChatMemory（实际存储）
     */
    private final ChatMemory chatMemory;

    /**
     * 用于生成摘要的 AI 模型（通义千问）
     */
    private final ChatModel chatModel;

    /**
     * 触发摘要的对话长度阈值（默认15条）
     */
    private final int summarizationThreshold;

    /**
     * 每次摘要时，从历史记录开头取出的消息数量（默认6条）
     */
    private final int messagesToSummarize;

    public SummarizingChatMemoryDecorator(
            ChatMemory chatMemory,
            ChatModel chatModel,
            int summarizationThreshold,
            int messagesToSummarize) {
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.summarizationThreshold = summarizationThreshold;
        this.messagesToSummarize = messagesToSummarize;
        
        log.info("📝 初始化记忆摘要装饰器: 阈值={}, 摘要消息数={}", 
                summarizationThreshold, messagesToSummarize);
    }

    /**
     * 添加消息，并异步触发摘要检查
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        // 1. 先保存消息到底层存储
        chatMemory.add(conversationId, messages);

        // 2. 异步执行摘要（不阻塞用户请求）
        CompletableFuture.runAsync(() -> summarize(conversationId))
                .exceptionally(e -> {
                    log.error("❌ 会话 {} 的异步摘要任务执行失败", conversationId, e);
                    return null;
                });
    }

    /**
     * 实际的摘要逻辑
     */
    private void summarize(String conversationId) {
        List<Message> currentHistory = chatMemory.get(conversationId);
        
        // 检查是否需要摘要
        if (currentHistory.size() <= summarizationThreshold) {
            return;
        }

        log.info("📊 会话 {} 的历史记录长度 {} 已超过阈值 {}，开始执行摘要...",
                conversationId, currentHistory.size(), summarizationThreshold);

        try {
            // 1. 提取需要被摘要的旧消息和需要保留的新消息
            List<Message> oldMessages = new ArrayList<>(
                    currentHistory.subList(0, messagesToSummarize));
            List<Message> recentMessages = new ArrayList<>(
                    currentHistory.subList(messagesToSummarize, currentHistory.size()));

            // 2. 创建摘要 Prompt
            String historyToSummarize = oldMessages.stream()
                    .map(message -> message.getMessageType().getValue() + ": " + message.getText())
                    .collect(Collectors.joining("\n"));

            String promptTemplate = """
                    请将以下多轮对话历史进行简洁的摘要，浓缩关键信息和上下文。
                    摘要应作为后续对话的背景知识，帮助 AI 理解长期的对话语境。
                    
                    对话历史:
                    ---
                    %s
                    ---
                    
                    请生成简洁的摘要（100字以内）：
                    """.formatted(historyToSummarize);

            // 3. 调用 AI 模型生成摘要
            Prompt summaryPrompt = new Prompt(promptTemplate);
            String summaryText = chatModel.call(summaryPrompt)
                    .getResult()
                    .getOutput()
                    .getText();
            
            Message summaryMessage = new SystemMessage("【历史对话摘要】: " + summaryText);
            log.info("✅ 会话 {} 的新摘要: {}", conversationId, summaryText);

            // 4. 构建新的、压缩后的对话历史 (摘要 + 近期消息)
            List<Message> newHistory = new ArrayList<>();
            newHistory.add(summaryMessage);
            newHistory.addAll(recentMessages);

            // 5. 原子性替换旧历史
            synchronized (this) {
                chatMemory.clear(conversationId);
                chatMemory.add(conversationId, newHistory);
            }
            
            log.info("✅ 会话 {} 的记忆摘要已完成。历史从 {} 条压缩到 {} 条",
                    conversationId, currentHistory.size(), newHistory.size());
                    
        } catch (Exception e) {
            log.error("❌ 会话 {} 的摘要生成失败", conversationId, e);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return chatMemory.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        chatMemory.clear(conversationId);
    }
}

