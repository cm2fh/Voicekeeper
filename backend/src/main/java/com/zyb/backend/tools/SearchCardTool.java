package com.zyb.backend.tools;

import com.zyb.backend.model.entity.VoiceCard;
import com.zyb.backend.service.VoiceCardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SearchCardTool {

    @Resource
    private VoiceCardService voiceCardService;

    @Tool(description = """
        【精确查询】查询用户的声音卡片列表
        使用时机：
        - 用户要"播放"、"找"、"查看"已有卡片
        - 需要按场景筛选：早安→morning 晚安→night 鼓励→encourage 思念→miss
        
        重要：查询特定声音的卡片时，必须传voiceModelId！
        流程：
        1. 先 searchVoiceModelByName 获取 modelId
        2. 再调用此工具，传入 voiceModelId 参数
        3. 这样只返回该声音的卡片，避免混淆不同声音的卡片
        """)
    public String searchUserCards(@ToolParam(description = "用户ID") Long userId,
                                  @ToolParam(description = "场景标签（可选）：morning/night/encourage/miss/custom，不传则查询所有") String sceneTag,
                                  @ToolParam(description = "声音模型ID（可选）：传入后只返回该声音的卡片，用于查询特定声音的卡片") Long voiceModelId) {
        List<VoiceCard> cards;
        if (sceneTag != null && !sceneTag.isEmpty()) {
            cards = voiceCardService.listByUserIdAndScene(userId, sceneTag);
        } else {
            cards = voiceCardService.listByUserId(userId);
        }
        
        // 按voiceModelId过滤
        if (voiceModelId != null) {
            cards = cards.stream()
                    .filter(card -> voiceModelId.equals(card.getVoiceModelId()))
                    .toList();
        }

        if (cards.isEmpty()) {
            if (voiceModelId != null) {
                return "没有找到该声音的卡片。";
            }
            return sceneTag == null ?
                    "用户还没有创建声音卡片。" :
                    "用户还没有'" + getSceneText(sceneTag) + "'场景的卡片。";
        }

        StringBuilder result = new StringBuilder();
        result.append(sceneTag == null ?
                "用户的所有声音卡片共" + cards.size() + "张：\n\n" :
                "用户的" + getSceneText(sceneTag) + "卡片共" + cards.size() + "张：\n\n");

        for (int i = 0; i < cards.size(); i ++) {
            VoiceCard card = cards.get(i);
            result.append(String.format(
                    """
                    %d. 【%s】
                       - 卡片ID: %d
                       - 音频URL: %s
                       - 场景: %s
                       - 内容: %s
                       - 播放次数: %d次

                    """,
                    i + 1,
                    card.getCardTitle(),
                    card.getId(),
                    card.getAudioUrl(),
                    getSceneText(card.getSceneTag()),
                    card.getTextContent().substring(0, Math.min(50, card.getTextContent().length())) +
                            (card.getTextContent().length() > 50 ? "..." : ""),
                    card.getPlayCount() == null ? 0 : card.getPlayCount()
            ));
        }

        return result.toString();
    }

    private String getSceneText(String sceneTag) {
        return switch (sceneTag) {
            case "morning" -> "早安问候";
            case "night" -> "晚安问候";
            case "encourage" -> "鼓励支持";
            case "miss" -> "表达思念";
            default -> "自定义";
        };
    }
}
