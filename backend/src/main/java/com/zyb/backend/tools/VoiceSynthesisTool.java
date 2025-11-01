package com.zyb.backend.tools;

import cn.hutool.core.io.FileUtil;
import com.zyb.backend.manager.OssManager;
import com.zyb.backend.model.entity.VoiceModel;
import com.zyb.backend.service.CosyVoiceService;
import com.zyb.backend.service.VoiceModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 语音合成工具
 * 使用阿里云 CosyVoice-v3 将文字转换为克隆的声音
 */
@Component
@Slf4j
public class VoiceSynthesisTool {

    @Resource
    private VoiceModelService voiceModelService;

    @Resource
    private CosyVoiceService cosyVoiceService;

    @Resource
    private OssManager ossManager;

    @Value("${aliyun.oss.url-prefix}")
    private String ossUrlPrefix;

    @Tool(description = """
            使用克隆的声音将文字合成为语音，支持情感控制。
            根据场景自动调整语速、音调和情感。
            返回生成的音频OSS地址。
            """)
    public String synthesizeVoice(
            @ToolParam(description = "声音模型ID") Long voiceModelId,
            @ToolParam(description = "要合成的文字内容（建议100字以内）") String text,
            @ToolParam(description = "场景标签：morning/night/encourage/miss/custom") String sceneTag
    ) {
        try {
            log.info("开始语音合成: modelId={}, 文字长度={}", voiceModelId, text.length());

            // 1. 获取声音模型
            VoiceModel model = voiceModelService.getById(voiceModelId);
            if (model == null) {
                return "错误：声音模型不存在（ID: " + voiceModelId + "）";
            }

            // 2. 检查模型状态
            if (model.getTrainingStatus() != 2) {
                return String.format(
                        """
                        错误：声音模型'%s'尚未就绪。
                        当前状态：%s
                        请等待处理完成后再使用。
                        """,
                    model.getModelName(),
                    getStatusText(model.getTrainingStatus())
                );
            }

            // 3. 根据场景获取情感参数
            EmotionParams emotionParams = getEmotionParams(sceneTag);

            // 4. 调用 CosyVoice 合成语音（带情感控制）
            byte[] audioData = cosyVoiceService.synthesizeSpeech(
                model.getAiModelId(),
                text,
                emotionParams.emotion,
                emotionParams.speechRate,
                emotionParams.pitchRate
            );

            // 5. 上传到OSS
            String fileName = "voice_" + System.currentTimeMillis() + ".mp3";
            String filePath = String.format(
                "voice_generated/%d/%s", 
                voiceModelId,
                fileName
            );

            // 创建临时文件
            File tempFile = File.createTempFile("voice_", ".mp3");
            FileUtil.writeBytes(audioData, tempFile);

            // 上传到OSS
            ossManager.putObject(filePath, tempFile);

            // 删除临时文件
            boolean deleted = tempFile.delete();
            if (!deleted) {
                log.warn("临时文件删除失败: {}", tempFile.getPath());
            }

            String audioUrl = ossUrlPrefix + filePath;

            // 6. 增加模型使用次数
            voiceModelService.increaseUseCount(voiceModelId);

            log.info("语音合成完成: audioUrl={}", audioUrl);

            // 7. 返回结果
            return String.format(
                    """
                    语音合成成功！
                    - 使用声音：%s
                    - 文字内容：%s
                    - 音频地址：%s
                    """,
                model.getModelName(),
                text.length() > 50 ? text.substring(0, 50) + "..." : text,
                audioUrl
            );

        } catch (Exception e) {
            log.error("语音合成失败", e);
            return "语音合成失败：" + e.getMessage() +
                   "\n请检查声音模型是否正常，或稍后重试。";
        }
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }

        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "失败";
            default -> "未知";
        };
    }

    /**
     * 根据场景获取情感参数
     */
    private EmotionParams getEmotionParams(String sceneTag) {
        if (sceneTag == null) {
            return new EmotionParams("neutral", 1.0f, 1.0f);
        }

        return switch (sceneTag.toLowerCase()) {
            case "morning" -> 
                // 早安：开心、快速、高音调
                new EmotionParams("happy", 1.1f, 1.0f);
                
            case "night" -> 
                // 晚安：平静、慢速、低音调
                new EmotionParams("neutral", 1.0f, 1.0f);
                
            case "encourage" -> 
                // 鼓励：开心、中速、略高音调
                new EmotionParams("happy", 1.1f, 1.05f);
                
            case "miss" -> 
                // 思念：悲伤、慢速、正常音调
                new EmotionParams("sad", 1.0f, 1.0f);
                
            default -> 
                new EmotionParams("neutral", 1.05f, 1.0f);
        };
    }

    /**
     * 情感参数封装
     */
    private static class EmotionParams {
        String emotion;      // 情感描述
        Float speechRate;    // 语速
        Float pitchRate;     // 音调

        EmotionParams(String emotion, Float speechRate, Float pitchRate) {
            this.emotion = emotion;
            this.speechRate = speechRate;
            this.pitchRate = pitchRate;
        }
    }
}