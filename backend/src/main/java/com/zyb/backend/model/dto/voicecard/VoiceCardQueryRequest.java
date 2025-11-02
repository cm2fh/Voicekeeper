package com.zyb.backend.model.dto.voicecard;

import com.zyb.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 声音卡片查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VoiceCardQueryRequest extends PageRequest implements Serializable {

    /**
     * 卡片ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 声音模型ID
     */
    private Long voiceModelId;

    /**
     * 卡片标题（模糊搜索）
     */
    private String cardTitle;

    /**
     * 场景标签
     */
    private String sceneTag;

    /**
     * 情感标签
     */
    private String emotionTag;

    /**
     * 是否AI生成
     */
    private Integer aiGenerated;

    /**
     * 搜索关键词（语义搜索）
     */
    private String searchText;

    @Serial
    private static final long serialVersionUID = 1L;
}

