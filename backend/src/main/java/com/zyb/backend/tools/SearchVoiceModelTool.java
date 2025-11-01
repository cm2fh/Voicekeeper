package com.zyb.backend.tools;

import com.zyb.backend.model.entity.VoiceModel;
import com.zyb.backend.service.VoiceModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SearchVoiceModelTool {

    @Resource
    private VoiceModelService voiceModelService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Tool(description = "查询用户的声音模型列表，获取所有可用的声音模型信息")
    public String searchUserVoiceModels(@ToolParam(description = "用户ID") Long userId) {
        List<VoiceModel> voiceModels = voiceModelService.listByUserId(userId);
        if (voiceModels.isEmpty()) {
            return "用户还没有声音模型，需要先上传音频样本进行声音克隆";
        }

        StringBuilder result = new StringBuilder("用户的声音模型列表：\n\n");
        for (int i = 0; i < voiceModels.size(); i ++) {
            VoiceModel model = voiceModels.get(i);
            result.append(String.format(
                    """
                    %d. 【%s】
                       - 模型ID: %d
                       - CosyVoice音色ID: %s
                       - 状态: %s
                       - 使用次数: %d次
                       - 创建时间: %s
                    """,
                    i + 1,
                    model.getModelName(),
                    model.getId(),
                    model.getAiModelId(),
                    getStatusText(model.getTrainingStatus()),
                    model.getUseCount() == null ? 0 : model.getUseCount(),
                    model.getCreateTime()
            ));
        }

        return result.toString();
    }

    @Tool(description = """
            根据声音名称查询特定的声音模型。
            
            重要：会检测声音克隆状态的变化。
            - 如果模型刚刚克隆完成（状态从'处理中'变为'已完成'），会特别提示
            - 用于实时通知用户声音克隆已完成
            """)
    public String searchVoiceModelByName(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "声音名称，如'妈妈的声音'") String modelName) {
        
        VoiceModel model = voiceModelService.getByUserIdAndName(userId, modelName);
        if (model == null) {
            return "未找到名为'" + modelName + "'的声音模型。用户需要先创建这个声音模型。";
        }

        // 检测状态变化（用Redis缓存上次状态）
        String statusKey = "voice:status:" + model.getId();
        Integer lastStatus = (Integer) redisTemplate.opsForValue().get(statusKey);
        Integer currentStatus = model.getTrainingStatus();

        // 更新Redis中的状态（5分钟过期）
        redisTemplate.opsForValue().set(statusKey, currentStatus, 5, TimeUnit.MINUTES);

        // 检测从"处理中"→"已完成"的变化
        boolean justCompleted = (lastStatus != null && lastStatus == 1 && currentStatus == 2);

        String baseInfo = String.format(
                """
                声音模型【%s】：
                - 模型ID: %d
                - 音色ID: %s
                - 状态: %s
                - 声音描述: %s
                - 使用次数: %d次
                """,
                model.getModelName(),
                model.getId(),
                model.getAiModelId(),
                getStatusText(currentStatus),
                model.getVoiceDesc(),
                model.getUseCount() == null ? 0 : model.getUseCount()
        );

        // 如果刚刚完成，添加特殊提示
        if (justCompleted) {
            return "🎉 好消息！声音克隆刚刚完成！\n\n" + baseInfo +
                   "\n✅ 现在可以立即使用这个声音创建卡片了！";
        }

        return baseInfo;
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
}
