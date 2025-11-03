package com.zyb.backend.tools;

import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.model.entity.VoiceCard;
import com.zyb.backend.service.VoiceCardService;
import com.zyb.backend.service.VoiceCardVectorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class VoiceCardCreateTool {

    @Resource
    private VoiceCardService voiceCardService;

    @Resource
    private VoiceCardVectorService vectorService;

    @Tool(description = """
        【创建卡片】将合成的语音保存为卡片
        使用时机：
        - 已调用 synthesizeVoice 获得 audioUrl 后
        - 确认用户要"创建"、"做"新卡片
        禁止：
        - 用户只是想"播放"、"听"已有卡片时使用
        - 必须先用 searchUserCards 检查是否已存在同类卡片
        """)
    public String createVoiceCard(@ToolParam(description = "用户ID") Long userId,
                                  @ToolParam(description = "声音模型ID") Long voiceModelId,
                                  @ToolParam(description = "卡片标题，如'晚安问候'") String cardTitle,
                                  @ToolParam(description = "文字内容") String textContent,
                                  @ToolParam(description = "音频OSS地址") String audioUrl,
                                  @ToolParam(description = "场景标签：morning/night/encourage/miss/custom") String sceneTag,
                                  @ToolParam(description = "是否AI生成：1-是, 0-否") Integer aiGenerated) {
        try {
            log.info("创建声音卡片: userId={}, title={}", userId, cardTitle);
            VoiceCard voiceCard = new VoiceCard();
            voiceCard.setUserId(userId);
            voiceCard.setVoiceModelId(voiceModelId);
            voiceCard.setCardTitle(cardTitle);
            voiceCard.setTextContent(textContent);
            voiceCard.setAiGenerated(aiGenerated);
            voiceCard.setAudioUrl(audioUrl);
            voiceCard.setSceneTag(sceneTag);
            voiceCard.setPlayCount(0);
            voiceCard.setShareCount(0);

            boolean success = voiceCardService.save(voiceCard);
            if (success) {
                log.info("声音卡片创建成功: cardId={}", voiceCard.getId());
                
                // 异步索引到向量存储
                CompletableFuture.runAsync(() -> {
                    try {
                        vectorService.indexCard(voiceCard.getId());
                    } catch (Exception e) {
                        log.error("卡片向量索引失败: cardId={}, error={}", voiceCard.getId(), e.getMessage());
                    }
                });
                
                return String.format(
                        """
                        创建成功！
                        - 卡片ID：%d
                        - 标题：%s
                        - 场景：%s
                        - 内容：%s
                        - 音频地址：%s
                        """,
                        voiceCard.getId(),
                        cardTitle,
                        getSceneText(sceneTag),
                        textContent.length() > 50 ? textContent.substring(0, 50) + "..." : textContent,
                        audioUrl
                );
            } else {
                return "卡片创建失败，请重试。";
            }
        } catch (Exception e) {
            log.error("创建卡片失败", e);
            return "卡片创建失败：" + e.getMessage();
        }
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
