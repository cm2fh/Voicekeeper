package com.zyb.backend.agent;

import com.zyb.backend.agent.model.AgentState;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础智能体类，用于管理智能体状态和执行流程
 * AI原生应用的核心基类
 */
@Data
@Slf4j
@NoArgsConstructor
public abstract class BaseAgent {

    private String name;

    private String systemPrompt;

    private String nextStepPrompt;

    private AgentState state = AgentState.IDLE;

    private int maxSteps = 20;

    private int currentStep = 0;

    private int maxHistorySize = 20; // 对话历史窗口大小

    // LLM客户端
    private ChatClient chatClient;

    // 会话ID和记忆存储
    private String conversationId;
    private ChatMemory chatMemory;

    /**
     * 构造函数
     */
    public BaseAgent(String conversationId, ChatMemory chatMemory) {
        this.conversationId = conversationId;
        this.chatMemory = chatMemory;
    }

    /**
     * 设置会话信息
     */
    public void setConversationInfo(String conversationId, ChatMemory chatMemory) {
        this.conversationId = conversationId;
        this.chatMemory = chatMemory;
    }

    /**
     * 获取消息列表
     */
    protected List<Message> getMessageList() {
        if (chatMemory == null || conversationId == null) {
            return List.of();
        }
        return chatMemory.get(conversationId);
    }

    /**
     * 添加消息
     */
    protected void addMessage(Message message) {
        if (chatMemory != null && conversationId != null && message != null) {
            chatMemory.add(conversationId, message);
        }
    }

    /**
     * 设置消息列表
     */
    protected void setMessageList(List<Message> messages) {
        if (chatMemory != null && conversationId != null && messages != null) {
            chatMemory.clear(conversationId);
            chatMemory.add(conversationId, messages);
        }
    }

    /**
     * 运行智能体（同步）
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("无法从当前状态运行智能体: " + this.state);
        }
        if (!StringUtils.hasText(userPrompt)) {
            throw new RuntimeException("无法在用户提示为空时运行智能体");
        }

        state = AgentState.RUNNING;
        addMessage(new UserMessage(userPrompt));

        StringBuilder results = new StringBuilder();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("执行步骤 {}/{}", stepNumber, maxSteps);
                
                // 单步执行
                String stepResult = step();
                results.append("Step ").append(stepNumber).append(": ").append(stepResult).append("\n");
                
                // 修剪历史
                pruneHistory();
            }

            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.append("执行结束: 达到最大步骤 (").append(maxSteps).append(")");
            }
            return results.toString();
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("执行智能体时发生错误", e);
            return "执行错误: " + e.getMessage();
        } finally {
            this.cleanup();
        }
    }

    /**
     * 运行智能体（流式输出）
     * AI原生应用的核心交互方式
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建SseEmitter
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：无法从状态运行智能体: " + this.state);
                    emitter.complete();
                    return;
                }
                if (!StringUtils.hasText(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行智能体");
                    emitter.complete();
                    return;
                }

                // 更改状态
                state = AgentState.RUNNING;
                // 记录消息上下文
                addMessage(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("执行步骤: {}/{}", stepNumber, maxSteps);

                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;

                        // 发送每一步的结果
                        emitter.send(result);
                        // 修剪历史
                        pruneHistory();
                    }
                    
                    // 检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE 连接超时");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE 连接完成");
        });

        return emitter;
    }

    /**
     * 执行单个步骤（子类实现）
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        if (state != AgentState.ERROR) {
            state = AgentState.IDLE;
        }
        currentStep = 0;
    }

    /**
     * 修剪对话历史，保留最后N条消息
     */
    protected void pruneHistory() {
        List<Message> messages = getMessageList();
        if (messages.size() <= maxHistorySize) {
            return;
        }
        log.info("对话历史较长，当前大小: {}", messages.size());
    }
}

