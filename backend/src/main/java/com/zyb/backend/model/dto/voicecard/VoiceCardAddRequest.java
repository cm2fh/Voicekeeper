package com.zyb.backend.model.dto.voicecard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 声音卡片创建请求
 */
@Data
public class VoiceCardAddRequest implements Serializable {

    /**
     * 声音模型ID
     */
    @NotNull(message = "声音模型ID不能为空")
    private Long voiceModelId;

    /**
     * 卡片标题
     */
    @NotBlank(message = "卡片标题不能为空")
    private String cardTitle;

    /**
     * 文字内容
     */
    @NotBlank(message = "文字内容不能为空")
    private String textContent;

    /**
     * 场景标签
     */
    private String sceneTag;

    /**
     * 情感标签
     */
    private String emotionTag;

    /**
     * 是否AI生成: 0-否 1-是
     */
    private Integer aiGenerated;

    @Serial
    private static final long serialVersionUID = 1L;
}

