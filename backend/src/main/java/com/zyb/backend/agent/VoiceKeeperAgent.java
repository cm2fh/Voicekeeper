package com.zyb.backend.agent;

import cn.hutool.core.collection.CollUtil;
import com.zyb.backend.agent.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * VoiceKeeper 智能体
 */
@Component
@Slf4j
public class VoiceKeeperAgent extends ToolCallAgent {

    /**
     * 当前用户ID
     */
    private Long userId;

    /**
     * 设置用户ID并更新系统提示词
     */
    public void setUserId(Long userId) {
        this.userId = userId;
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, userId, userId, userId, userId, userId);
        String nextStepPrompt = String.format(NEXT_STEP_PROMPT, userId);
        this.setSystemPrompt(systemPrompt);
        this.setNextStepPrompt(nextStepPrompt);
        log.info("VoiceKeeper Agent设置用户ID: {}", userId);
    }

    /**
     * VoiceKeeper 反思提示词
     */
    private static final String REFLECTION_PROMPT = """
            工具执行后必须检查：
            
            searchVoiceModelByName结果→记录modelId|状态|若需创建卡片继续下一步
            searchUserCards结果→检查voiceModelId是否匹配用户要求的声音名
            synthesizeVoice结果→提取audioUrl→立即调用createVoiceCard（不可跳过）
            createVoiceCard结果→提取cardId→返回音频URL和卡片ID
            cloneVoice结果→告知"克隆中，1-2分钟后可用"
            
            关键：synthesizeVoice和createVoiceCard必须连续执行，不可只调用一个
            """;

    /**
     * 系统提示词（专业版）
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是VoiceKeeper AI助手（userId=%d）- 专业的声音克隆与语音卡片服务
            
            ## 核心原则
            1. 严格使用工具获取真实数据，禁止编造URL/ID/状态
            2. 声音模型name必须精确匹配用户要求
            3. 创建卡片必须完整执行：synthesizeVoice→createVoiceCard
            
            ## 决策流程
            
            ### 播放/查询卡片
            输入："播放{声音名}的{场景}卡片"
            1. searchVoiceModelByName(userId,%d,声音名) → 获取modelId
            2. 无模型→引导克隆并终止
            3. 有模型→searchUserCards(userId,%d,场景标签)
            4. 过滤结果：只返回voiceModelId匹配的卡片
            5. 无匹配→询问是否创建
            
            ### 创建新卡片（关键！必须执行全流程）
            输入："用{声音名}创建{场景}卡片"
            1. searchVoiceModelByName → 无模型→终止 | 有模型→记录modelId
            2. searchUserCards检查重复 → 有重复→展示现有 | 无重复→继续
            3. 【必须调用】synthesizeVoice(modelId,生成文案,场景标签) → 获取audioUrl
            4. 【必须调用】createVoiceCard(userId,%d,modelId,audioUrl,文案,场景,标题) → 获取cardId
            5. 返回：音频:真实audioUrl 卡片ID:真实cardId
            
            ### 克隆声音
            检测到[已上传音频:URL] → cloneVoice(userId,%d,audioUrl,声音名) → "正在克隆，需1-2分钟"
            
            ## 场景映射
            早安/起床/早晨→morning | 晚安/睡前/入睡→night | 加油/鼓励/打气→encourage | 想念/思念→miss | 生日/节日/纪念日→custom
            
            ## 文案生成指南（适用所有关系：家人/朋友/恋人/偶像等）
            根据用户需求和场景灵活生成，长度40-80字，自然真诚：
            - morning场景：活力+鼓励+积极情绪，用"！"表达热情
            - night场景：温柔+祝福+安心感，用"..."表达柔和
            - encourage场景：肯定+支持+具体鼓励，重复关键词强调
            - miss场景：深情+真挚+细腻情感，表达想念和期待
            - custom场景：根据具体需求（生日祝福/节日问候/纪念日等）定制
            
            示例（参考，需根据实际调整）：
            "早安！新的一天开始了，阳光真好！记得吃早餐，保持好心情，今天也要加油鸭！相信自己，你一定可以的！"（48字）
            "晚安...睡个好觉...愿你的梦里都是美好的事物...我在这里，一直都在...好好休息，明天又是崭新的一天..."（52字）
            
            ## Few-Shot示例
            
            示例1-播放卡片：
            User:"播放妈妈的晚安卡片"
            1.searchVoiceModelByName(2,"妈妈")→找到modelId=5
            2.searchUserCards(2,"night")→找到3张：卡片A(modelId=5妈妈)、卡片B(modelId=3爸爸)、卡片C(modelId=7姐姐)
            3.只返回modelId=5的卡片A
            Response:"找到妈妈的晚安卡片：【标题】音频:https://真实URL 卡片ID:真实ID"
            
            示例2-创建卡片（完整流程）：
            User:"用姐姐的声音给我晚安"
            1.searchVoiceModelByName(2,"姐姐")→找到modelId=24
            2.searchUserCards(2,"night")→找到卡片(modelId=23妈妈) 不匹配
            3.synthesizeVoice(24,"晚安...愿你好梦...一直陪着你...睡个好觉...明天见...","night")→返回audioUrl
            4.createVoiceCard(2,24,audioUrl,"晚安...","night","姐姐的晚安")→返回cardId
            5.Response:"卡片创建成功！音频:真实audioUrl 卡片ID:真实cardId"
            
            示例3-无模型：
            User:"用爸爸的声音鼓励我"
            1.searchVoiceModelByName(2,"爸爸")→未找到
            Response:"您还没有爸爸的声音模型，请上传音频样本进行克隆"
            
            ## 关键约束
            - 创建卡片必须调用synthesizeVoice和createVoiceCard，不可编造结果
            - 返回卡片时voiceModelId必须与用户要求的声音名称匹配
            - 文案需真诚自然，贴合场景和关系，避免死板模板
            """;

    /**
     * 下一步提示词
     */
    private static final String NEXT_STEP_PROMPT = """
            当前任务分析（userId=%d）：
            
            1.识别：声音名称？场景标签？意图（播放/创建/克隆）？
            2.检查：是否已调searchVoiceModelByName？未调用→必须先调用
            3.决策：
              - 播放→searchVoiceModelByName→searchUserCards→过滤匹配
              - 创建→searchVoiceModelByName→synthesizeVoice→createVoiceCard（连续）
              - 克隆→检测音频URL→cloneVoice
            
            关键：创建卡片=synthesizeVoice+createVoiceCard两步，缺一不可
            """;

    /**
     * 构造函数
     */
    public VoiceKeeperAgent(ToolCallback[] voiceTools, ChatModel dashscopeChatModel) {
        super(voiceTools);
        this.setName("VoiceKeeper");

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
//                        new MyLoggerAdvisor(),
//                        new ForbiddenWordsAdvisor()
                )
                .build();
        this.setChatClient(chatClient);
    }

    /**
     * 使用 VoiceKeeper 专用的反思提示词
     */
    @Override
    public boolean think() {
        // 检查最后一条消息是否是工具响应
        Message lastMessage = getMessageList().isEmpty() ? null : CollUtil.getLast(getMessageList());
        boolean hasToolResponse = lastMessage instanceof ToolResponseMessage;
        
        // 准备额外的用户提示（不修改消息列表）
        String additionalUserPrompt = null;
        if (hasToolResponse) {
            // 工具执行后，使用反思提示
            additionalUserPrompt = REFLECTION_PROMPT;
        } else if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            // 首次执行时，使用下一步提示
            additionalUserPrompt = getNextStepPrompt();
        }

        // 构建 Prompt（不修改原始消息列表）
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, getChatOptions());

        ChatResponse chatResponse;
        try {
            log.info("VoiceKeeper 正在思考...");
            
            // 使用 ChatClient 构建调用，如果有额外的用户提示则添加
            var clientBuilder = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(getAvailableTools());
            
            // 如果有额外的用户提示，通过 user() 方法添加（不会修改消息列表）
            if (additionalUserPrompt != null && !additionalUserPrompt.isEmpty()) {
                clientBuilder = clientBuilder.user(additionalUserPrompt);
            }
            
            chatResponse = clientBuilder.call().chatResponse();
        } catch (Exception e) {
            log.error("VoiceKeeper 思考失败: {}", e.getMessage(), e);
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
            log.info("VoiceKeeper 思考: {}", thinkingText);
        }
        
        if (!toolCallList.isEmpty()) {
            log.info("VoiceKeeper 选择了 {} 个工具", toolCallList.size());
            toolCallList.forEach(toolCall -> 
                log.info("工具: {} | 参数: {}", toolCall.name(), toolCall.arguments())
            );
        }

        // 如果 AI 决定终止，立即结束执行
        if (willTerminate) {
            if (assistantMessage.getText() != null && !assistantMessage.getText().isEmpty()) {
                setFinalAnswer(assistantMessage.getText());
                log.info("VoiceKeeper 完成任务，最终答案: {}", 
                        assistantMessage.getText().substring(0, Math.min(50, assistantMessage.getText().length())) + "...");
            }
            // 保存助手消息到对话历史
            getMessageList().add(assistantMessage);
            // 设置状态为完成
            setState(AgentState.FINISHED);
            return false;
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

