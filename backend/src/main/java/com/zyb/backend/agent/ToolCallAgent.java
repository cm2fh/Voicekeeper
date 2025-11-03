package com.zyb.backend.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zyb.backend.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具调用智能体
 * AI原生应用的核心：AI自主决策调用工具完成任务
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private static final String REFLECTION_PROMPT = """
            你刚刚执行了一个工具调用，现在需要对结果进行反思。
            请仔细分析上一步的工具调用结果。
            1. **评估**：工具调用是否成功？它的输出是否让我们离最终答案更近了一步？
            2. **批判**：这是解决问题的最佳工具和最佳参数吗？我最初的计划是否仍然是最好的？
            3. **规划**：基于你的评估，明确阐述下一步的具体行动。这可以是调用另一个工具、修正计划、向用户提问，或者如果信息足够，就直接给出最终答案。
            
            现在，请继续执行你规划的下一步。
            """;

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;

    // 循环检测相关
    private List<String> recentActions = new ArrayList<>();
    private int maxRecentActions = 10; // 记录最近的操作数量
    private int loopThreshold = 3; // 检测到循环的阈值

    // 重试机制相关
    private int maxRetries = 3; // 最大重试次数
    private long retryDelayMillis = 1000; // 重试间隔

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 思考：处理当前状态并决定下一步行动
     * AI原生应用的核心：AI自己决定做什么
     */
    @Override
    public boolean think() {
        // 检查最后一条消息是否是工具响应，如果是，则注入反思提示
        Message lastMessage = getMessageList().isEmpty() ? null : CollUtil.getLast(getMessageList());
        if (lastMessage instanceof ToolResponseMessage) {
            UserMessage reflectionMessage = new UserMessage(REFLECTION_PROMPT);
            getMessageList().add(reflectionMessage);
        } else if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            // 否则，使用标准的下一步提示（在用户输入后的第一步）
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);

        // 调用大模型API，带重试机制
        ChatResponse chatResponse = null;
        int attempt = 0;
        while (true) {
            try {
                log.info("调用通义千问LLM (尝试次数: {})", attempt + 1);
                chatResponse = getChatClient().prompt(prompt)
                        .system(getSystemPrompt())
                        .toolCallbacks(availableTools)
                        .call()
                        .chatResponse();
                break;
            } catch (Exception e) {
                log.error("{}的思考过程遇到了问题: {}", getName(), e.getMessage());
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("API 调用失败，已达到最大重试次数 ({})", maxRetries);
                    getMessageList().add(new AssistantMessage("处理时遇到错误: " + e.getMessage()));
                    return false;
                }
                log.info("🔄 将在 {} 毫秒后重试... (第 {}/{}次)", retryDelayMillis, attempt, maxRetries);
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    getMessageList().add(new AssistantMessage("重试等待时被中断"));
                    return false;
                }
            }
        }

        // 记录响应，用于Act方法
        this.toolCallChatResponse = chatResponse;
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

        // 检查是否需要终止
        boolean willTerminate = assistantMessage.getToolCalls().stream()
                .anyMatch(toolCall -> "doTerminate".equalsIgnoreCase(toolCall.name()));

        // 输出思考信息
        String result = assistantMessage.getText();
        List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
        log.info("{}思考: {}", getName(), result);
        log.info("{}选择了 {} 个工具", getName(), toolCallList.size());
        
        if (!toolCallList.isEmpty()) {
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format(" 工具：%s | 参数：%s",
                            toolCall.name(),
                            toolCall.arguments())
                    )
                    .collect(Collectors.joining("\n"));
            log.info("工具调用详情:\n{}", toolCallInfo);
        }

        // 如果AI决定终止，立即结束执行
        if (willTerminate) {
            if (assistantMessage.getText() != null && !assistantMessage.getText().isEmpty()) {
                setFinalAnswer(assistantMessage.getText());
                log.info("{}设置最终答案: {}", getName(), assistantMessage.getText());
            }
            // 保存助手消息到对话历史
            getMessageList().add(assistantMessage);
            // 立即设置状态为完成，不再执行act()
            setState(AgentState.FINISHED);
            return false; // 不执行act()，直接结束
        }

        if (toolCallList.isEmpty()) {
            // 只有不调用工具时，才记录助手消息
            getMessageList().add(assistantMessage);
            setFinalAnswer(assistantMessage.getText());
            return false;
        } else {
            // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
            return true;
        }
    }

    /**
     * 行动：执行工具调用并处理结果
     * AI原生应用的执行层
     */
    @Override
    public String act() {
        if (toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        Message lastMessage = CollUtil.getLast(toolExecutionResult.conversationHistory());
        if (!(lastMessage instanceof ToolResponseMessage)) {
            setMessageList(toolExecutionResult.conversationHistory());
            return "工具调用已执行，但未找到预期的工具响应";
        }

        ToolResponseMessage originalToolResponse = (ToolResponseMessage) lastMessage;
        List<ToolResponseMessage.ToolResponse> cleanedResponses = new ArrayList<>();

        // 清理工具响应内容
        for (ToolResponseMessage.ToolResponse response : originalToolResponse.getResponses()) {
            String originalData = response.responseData();
            // 1. 仅移除多余换行，保留所有其他内容（包括URL、下划线等）
            String cleanedData = originalData.replaceAll("\\n{3,}", "\n\n").trim();

            cleanedResponses.add(new ToolResponseMessage.ToolResponse(response.id(), response.name(), cleanedData));
        }

        ToolResponseMessage cleanedToolResponseMessage = new ToolResponseMessage(cleanedResponses);
        List<Message> newHistory = new ArrayList<>(toolExecutionResult.conversationHistory());
        newHistory.set(newHistory.size() - 1, cleanedToolResponseMessage);
        setMessageList(newHistory);

        String resultsForLog = cleanedResponses.stream()
                .map(response -> String.format(" 工具 %s 完成任务！结果: %s",
                        response.name(),
                        response.responseData().substring(0, Math.min(200, response.responseData().length())) + "..."))
                .collect(Collectors.joining("\n"));
        log.info("工具执行结果:\n{}", resultsForLog);

        // 循环检测
        String actionSignature = cleanedResponses.stream()
                .map(response -> response.name() + ":" + response.responseData().substring(0,
                        Math.min(50, response.responseData().length())))
                .collect(Collectors.joining("|"));

        recentActions.add(actionSignature);
        if (recentActions.size() > maxRecentActions) {
            recentActions.remove(0);
        }

        if (detectLoop()) {
            String correctionMessage = "[系统自我纠正]: 检测到循环模式，重新评估策略";
            getMessageList().add(new AssistantMessage(correctionMessage));
            recentActions.clear();
            return "检测到操作循环模式，将尝试自我纠错";
        }

        return "工具调用成功";
    }

    /**
     * 检测循环模式
     * 简单实现：检查最近的操作中是否有连续重复的模式
     */
    private boolean detectLoop() {
        if (recentActions.size() < loopThreshold * 2) {
            return false; // 操作太少，无法形成循环
        }

        // 检查最近的操作是否形成循环模式
        for (int patternLength = 1; patternLength <= recentActions.size() / loopThreshold; patternLength++) {
            boolean isLoop = true;

            // 获取最近的模式
            List<String> pattern = recentActions.subList(
                    recentActions.size() - patternLength,
                    recentActions.size());

            // 检查这个模式是否重复出现至少loopThreshold次
            for (int i = 1; i < loopThreshold; i++) {
                int startIdx = recentActions.size() - (i + 1) * patternLength;
                if (startIdx < 0) {
                    isLoop = false;
                    break;
                }

                List<String> previousPattern = recentActions.subList(
                        startIdx,
                        startIdx + patternLength);

                if (!pattern.equals(previousPattern)) {
                    isLoop = false;
                    break;
                }
            }

            if (isLoop) {
                log.warn("⚠️ 检测到循环模式: {}", pattern);
                return true;
            }
        }

        return false;
    }

    /**
     * 清理资源
     */
    @Override
    protected void cleanup() {
        super.cleanup();
        recentActions.clear();
        toolCallChatResponse = null;
    }
}

