package com.zyb.backend.tools;

import com.zyb.backend.model.entity.VoiceModel;
import com.zyb.backend.service.VoiceModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SearchVoiceModelTool {

    @Resource
    private VoiceModelService voiceModelService;

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

    @Tool(description = "根据声音名称查询特定的声音模型")
    public String searchVoiceModelByName(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "声音名称，如妈妈的声音") String modelName) {
        
        VoiceModel model = voiceModelService.getByUserIdAndName(userId, modelName);
        if (model == null) {
            return "未找到声音模型。用户需要先创建声音模型。";
        }

        return String.format(
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
                getStatusText(model.getTrainingStatus()),
                model.getVoiceDesc(),
                model.getUseCount() == null ? 0 : model.getUseCount()
        );
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
