package com.zyb.backend.model.dto.voicecard;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 声音卡片更新请求
 */
@Data
public class VoiceCardUpdateRequest implements Serializable {

    /**
     * 卡片ID
     */
    @NotNull(message = "卡片ID不能为空")
    private Long id;

    /**
     * 卡片标题
     */
    private String cardTitle;

    /**
     * 文字内容
     */
    private String textContent;

    /**
     * 场景标签
     */
    private String sceneTag;

    /**
     * 情感标签
     */
    private String emotionTag;

    @Serial
    private static final long serialVersionUID = 1L;
}

