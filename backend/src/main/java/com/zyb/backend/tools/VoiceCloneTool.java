package com.zyb.backend.tools;

import com.zyb.backend.model.entity.VoiceModel;
import com.zyb.backend.service.CosyVoiceService;
import com.zyb.backend.service.VoiceModelService;
import com.zyb.backend.utils.ToolRetryHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 声音克隆工具
 * 使用阿里云 CosyVoice-v3 进行声音克隆
 */
@Component
@Slf4j
public class VoiceCloneTool {

    @Resource
    private VoiceModelService voiceModelService;

    @Resource
    private CosyVoiceService cosyVoiceService;

    @Resource
    private ToolRetryHelper retryHelper;

    @Tool(description = """
            克隆用户上传的声音样本，使用阿里云CosyVoice-v3进行声音克隆。
            要求：
            - 音频时长：10-20秒
            - 格式：WAV/MP3/M4A
            - 质量：清晰、无背景音、连续朗读
            返回克隆后的声音模型信息。
            """)
    public String cloneVoice(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "样本音频的阿里云OSS公网地址") String audioUrl,
            @ToolParam(description = "声音模型名称，如'妈妈的声音'") String modelName,
            @ToolParam(description = "声音描述（可选），如'温暖慈爱'") String voiceDesc
    ) {
        try {
            log.info("开始声音克隆: userId={}, modelName={}", userId, modelName);

            // 1. 检查是否已存在同名模型
            VoiceModel existingModel = voiceModelService.getByUserIdAndName(userId, modelName);
            if (existingModel != null) {
                return "错误：已存在名为" + modelName + "的声音模型，请使用其他名称。";
            }

            // 2. 调用LLM创建音色
            String voiceId = retryHelper.executeNetworkCall("createVoice", () ->
                cosyVoiceService.createVoice(audioUrl)
            );

            // 3. 保存到数据库
            VoiceModel model = new VoiceModel();
            model.setUserId(userId);
            model.setModelName(modelName);
            model.setVoiceDesc(voiceDesc);
            model.setSampleAudioUrl(audioUrl);
            model.setAiModelId(voiceId);
            model.setAiProvider("CosyVoice-v3");
            model.setTrainingStatus(1);
            model.setTrainingMessage("声音克隆中，请稍候...");
            model.setUseCount(0);

            voiceModelService.save(model);

            log.info("声音模型已创建: modelId={}, voiceId={}", model.getId(), voiceId);

            // 4. 异步等待音色就绪
            asyncWaitForVoice(model.getId(), voiceId);

            // 5. 返回结果
            return String.format(
                    """
                    声音克隆请求已提交，会在后台处理！
                    - 模型信息：
                    - 名称：%s
                    - 模型ID：%d
                    - CosyVoice音色ID：%s
                    大约需要1-2分钟，处理完成后即可使用这个声音创建卡片。
                    """,
                modelName,
                model.getId(),
                voiceId
            );

        } catch (Exception e) {
            log.error("声音克隆失败", e);
            return "声音克隆失败：" + e.getMessage() + 
                   "\n请检查音频URL是否正确，音频是否符合要求（10-20秒，清晰无噪音）";
        }
    }

    /**
     * 异步等待音色处理完成（Virtual Thread）
     */
    private void asyncWaitForVoice(Long modelId, String voiceId) {
        Thread.startVirtualThread(() -> {
            try {
                int maxAttempts = 20;
                int intervalSeconds = 5;

                for (int i = 0; i < maxAttempts; i++) {
                    String status = cosyVoiceService.queryVoiceStatus(voiceId);

                    if ("OK".equals(status)) {
                        // 成功：更新数据库
                        voiceModelService.updateTrainingStatus(
                            modelId, 2, "声音克隆成功！可以开始使用了"
                        );
                        log.info("音色处理完成: modelId={}, voiceId={}", modelId, voiceId);
                        return;
                    } else if ("UNDEPLOYED".equals(status)) {
                        // 失败：更新数据库
                        voiceModelService.updateTrainingStatus(
                            modelId, 3, "声音克隆失败，请检查音频质量"
                        );
                        log.error("音色处理失败: modelId={}, voiceId={}", modelId, voiceId);
                        return;
                    }

                    // 继续等待
                    Thread.sleep(intervalSeconds * 1000L);
                }

                // 超时：更新为失败
                voiceModelService.updateTrainingStatus(
                    modelId, 3, "处理超时，请重试"
                );
                log.error("音色处理超时: modelId={}", modelId);

            } catch (Exception e) {
                log.error("异步等待失败", e);
                try {
                    voiceModelService.updateTrainingStatus(
                        modelId, 3, "异常：" + e.getMessage()
                    );
                } catch (Exception ex) {
                    log.error("更新状态失败", ex);
                }
            }
        });
    }
}