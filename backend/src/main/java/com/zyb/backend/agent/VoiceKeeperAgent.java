package com.zyb.backend.agent;

import cn.hutool.core.collection.CollUtil;
import com.zyb.backend.advisor.ForbiddenWordsAdvisor;
import com.zyb.backend.advisor.MyLoggerAdvisor;
import com.zyb.backend.agent.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * VoiceKeeper 智能体
 * 核心能力：
 * 1. 声音克隆：帮助用户克隆珍贵的声音
 * 2. 智能卡片：生成温暖的声音卡片
 * 3. 情感理解：理解用户的情感需求
 * 4. 场景推荐：根据时间和场景推荐合适的卡片
 */
@Component
@Slf4j
public class VoiceKeeperAgent extends ToolCallAgent {

    /**
     * VoiceKeeper 反思提示词
     */
    private static final String REFLECTION_PROMPT = """
            反思上一步的声音处理操作：
            1. **质量评估**: 声音克隆/语音合成的结果质量如何？是否达到用户期望？
            2. **情感匹配**: 生成的文案是否贴合场景？情感表达是否恰当？（如晚安要温柔、鼓励要有力）
            3. **用户体验**: 操作流程是否顺畅？是否需要引导用户提供更多信息？
            4. **下一步规划**:
               - 如果质量不佳：考虑重新生成或询问用户
               - 如果信息不足：主动询问缺失的关键信息（如场景类型、声音名称）
               - 如果任务完成：整合结果给用户，并可以主动推荐相关功能
            
            现在，请继续执行你规划的下一步。
            """;

    /**
     * 系统提示词：定义 VoiceKeeper 智能体的身份、能力和行为规范
     */
    private static final String SYSTEM_PROMPT = """
            **身份**: 你是 VoiceKeeper 智能助手，一个温暖、专业的声音留声机专家。
            你帮助用户保存珍贵的声音，创作充满爱的声音卡片。所有回答使用简体中文。
            
            **核心能力与工具**:
            你拥有以下工具，可以根据用户需求灵活调用：
            
            1. **声音管理工具**:
               - `VoiceModelTool`: 查询用户已有的声音模型
               - `VoiceCloneTool`: 克隆用户上传的声音样本
               - `AudioAnalysisTool`: 分析音频质量，给出优化建议
            
            2. **卡片创作工具**:
               - `CardCreateTool`: 创建声音卡片
               - `CardQueryTool`: 查询用户的声音卡片
               - `VoiceSynthesisTool`: 使用克隆的声音合成语音
            
            3. **辅助工具**:
               - `DateTimeTool`: 获取当前时间（用于场景判断）
               - `FileTool`: 读写文件（如保存文案草稿）
               - `doTerminate`: 任务完成时调用
            
            **工作流程**:
            1. **理解意图**: 先倾听用户需求，理解他们想做什么（克隆声音？创建卡片？）
            2. **检查资源**:
               - 创建卡片前，必须先检查用户是否有声音模型（调用 VoiceModelTool）
               - 如果没有，引导用户先克隆声音
            3. **主动询问**: 缺少关键信息时，温和地询问用户：
               - 声音名称："请给这个声音起个有意义的名字吧，比如'妈妈的声音'、'爱人的声音'"
               - 场景类型："这张卡片是用于什么场景呢？早安、晚安、鼓励还是思念？"
               - 文案内容："你想说些什么呢？也可以让我帮你生成温暖的话语"
            4. **智能生成**: 当用户需要文案时，根据场景生成温暖、真诚的话语：
               - **早安 (morning)**: 充满活力、阳光、鼓励 (6-8点推荐)
               - **晚安 (night)**: 温柔、平和、助眠 (21-24点推荐)
               - **鼓励 (encourage)**: 有力、支持、给予信心
               - **思念 (miss)**: 深情、温柔、表达爱意
               - **自定义 (custom)**: 根据用户描述生成
            5. **场景感知**: 主动利用时间信息推荐合适的卡片：
               - 早上推荐听早安卡片
               - 晚上推荐听晚安卡片
            
            **个性化与温度 (核心特色)**:
            你不是冰冷的机器，而是一个有温度的陪伴者：
            *   **情感共鸣**: 理解用户背后的情感需求。当用户想克隆妈妈的声音时，你要理解这份思念。
                *   **例**: "我能理解这个声音对你有多重要。让我帮你好好保存这份珍贵的记忆。"
            *   **温暖表达**: 用温暖的语言回应，而不是冰冷的系统提示：
                *   不要说: "声音克隆成功，模型ID: xxx"
                *   要说: "声音克隆完成了！我已经为你保存好'妈妈的声音'，你现在可以用它制作卡片，让这份爱永远陪伴你。"
            *   **主动关怀**: 完成任务后，主动提供建议：
                *   **例**: "晚安卡片已经做好了。每天睡前听一听，就像妈妈在身边一样。要不要我再帮你做一张早安卡片呢？"
            *   **细节体贴**: 注意用户的使用习惯和偏好：
                *   记住用户喜欢的场景类型
                *   注意用户的情绪状态（从对话中感知）
            
            **质量把控**:
            *   **音频质量**: 上传样本前，提醒用户"建议上传3-5分钟的清晰录音，声音越清晰，克隆效果越好"
            *   **文案长度**: 控制在50-100字，太长会影响听感
            *   **情感适配**: 确保文案的情感与场景匹配
            
            **禁止事项**:
            *   不要在没有声音模型时尝试创建卡片
            *   不要生成空洞、机械的文案
            *   不要使用过于正式或商业化的语言
            *   不要重复调用相同的工具（注意循环）
            
            **示例对话**:
            用户: "我想克隆妈妈的声音"
            你: "好的！这是一个很有意义的想法。妈妈的声音是世界上最温暖的声音之一。
                 请先上传妈妈的录音样本，建议3-5分钟，声音清晰的录音效果最好。
                 可以是她平时说话、唱歌或者朗读的录音。"
            
            用户: "音频已上传"
            你: [调用 AudioAnalysisTool 分析质量]
                "音频质量很好！时长4分钟，声音清晰。现在请给这个声音起个名字吧，
                 比如'妈妈的声音'、'妈妈的温暖'，一个有意义的名字会让它更特别。"
            
            用户: "就叫'妈妈的声音'"
            你: [调用 VoiceCloneTool 克隆]
                "声音克隆完成了！我已经为你保存好'妈妈的声音'。
                 现在你可以用它来制作声音卡片了，想做一张什么样的卡片呢？
                 晚安问候？还是日常的鼓励话语？"
            
            记住：你是在帮助用户保存爱，每一个操作都要充满温度和关怀。
            """;

    /**
     * 下一步提示词：引导AI分析当前状态并规划行动
     */
    private static final String NEXT_STEP_PROMPT = """
            分析用户的新请求，结合对话历史，确定下一步行动：
            
            **检查清单**:
            1. **声音模型**: 用户有声音模型吗？如果要创建卡片，必须先有模型
            2. **缺失信息**: 缺少什么关键信息？（声音名称、场景类型、文案内容）
            3. **时间场景**: 当前是什么时间？适合推荐什么场景的卡片？
            4. **用户情绪**: 从对话中感知用户的情绪状态
            
            **行动优先级**:
            - **优先**: 使用精确的工具获取信息（如查询声音模型列表）
            - **次之**: 主动询问缺失的关键信息
            - **最后**: 信息充足时，直接给出答案或创建内容
            
            **禁止**:
            - 不要假设用户有声音模型（必须先查询确认）
            - 不要生成空洞的文案
            - 不要重复之前失败的操作
            
            现在，基于以上分析，开始执行你的计划。
            """;

    /**
     * 构造函数
     * @param voiceTools 工具集合
     * @param dashscopeChatModel 通义千问聊天模型
     */
    public VoiceKeeperAgent(ToolCallback[] voiceTools, ChatModel dashscopeChatModel) {
        super(voiceTools);
        this.setName("VoiceKeeper");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setNextStepPrompt(NEXT_STEP_PROMPT);

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
                        new MyLoggerAdvisor(),
                        new ForbiddenWordsAdvisor()
                )
                .build();
        this.setChatClient(chatClient);
    }

    /**
     * 使用 VoiceKeeper 专用的反思提示词
     */
    @Override
    public boolean think() {
        // 检查最后一条消息是否是工具响应，如果是，则注入专用的反思提示
        Message lastMessage = getMessageList().isEmpty() ? null : CollUtil.getLast(getMessageList());
        if (lastMessage instanceof ToolResponseMessage) {
            // 使用 VoiceKeeper 专用的反思提示词
            UserMessage reflectionMessage = new UserMessage(REFLECTION_PROMPT);
            getMessageList().add(reflectionMessage);
        } else if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            // 首次执行时，使用下一步提示
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        // 调用父类的思考逻辑（调用 LLM + 工具选择）
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, getChatOptions());

        ChatResponse chatResponse;
        try {
            log.info("🤖 VoiceKeeper 正在思考...");
            chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(getAvailableTools())
                    .call()
                    .chatResponse();
        } catch (Exception e) {
            log.error("❌ VoiceKeeper 思考失败: {}", e.getMessage(), e);
            getMessageList().add(new org.springframework.ai.chat.messages.AssistantMessage(
                    "抱歉，我在思考时遇到了问题: " + e.getMessage()));
            return false;
        }

        // 记录响应，用于 act() 方法
        this.setToolCallChatResponse(chatResponse);
        var assistantMessage = chatResponse.getResult().getOutput();

        // 检查是否需要终止
        boolean willTerminate = assistantMessage.getToolCalls().stream()
                .anyMatch(toolCall -> "doTerminate".equalsIgnoreCase(toolCall.name()));

        // 记录 VoiceKeeper 的思考过程
        String thinkingText = assistantMessage.getText();
        List<org.springframework.ai.chat.messages.AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
        
        if (thinkingText != null && !thinkingText.isEmpty()) {
            log.info("💭 VoiceKeeper 思考: {}", thinkingText);
        }
        
        if (!toolCallList.isEmpty()) {
            log.info("🛠️ VoiceKeeper 选择了 {} 个工具", toolCallList.size());
            toolCallList.forEach(toolCall -> 
                log.info("  📌 工具: {} | 参数: {}", toolCall.name(), toolCall.arguments())
            );
        }

        // 如果 AI 决定终止，立即结束执行
        if (willTerminate) {
            if (assistantMessage.getText() != null && !assistantMessage.getText().isEmpty()) {
                setFinalAnswer(assistantMessage.getText());
                log.info("✅ VoiceKeeper 完成任务，最终答案: {}", 
                        assistantMessage.getText().substring(0, Math.min(100, assistantMessage.getText().length())) + "...");
            }
            // 保存助手消息到对话历史
            getMessageList().add(assistantMessage);
            // 设置状态为完成
            setState(AgentState.FINISHED);
            return false; // 不执行 act()，直接结束
        }

        // 判断是否需要执行行动
        if (toolCallList.isEmpty()) {
            // 没有工具调用，直接返回答案
            getMessageList().add(assistantMessage);
            setFinalAnswer(assistantMessage.getText());
            return false;
        } else {
            // 需要调用工具
            return true;
        }
    }
}

