package com.zyb.backend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 声音卡片视图对象
 */
@Data
public class VoiceCardVO implements Serializable {

    /**
     * ID
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
     * 声音模型名称（关联查询）
     */
    private String voiceModelName;

    /**
     * 卡片标题
     */
    private String cardTitle;

    /**
     * 文字内容
     */
    private String textContent;

    /**
     * 是否AI生成: 0-否 1-是
     */
    private Integer aiGenerated;

    /**
     * 生成的音频OSS地址
     */
    private String audioUrl;

    /**
     * 音频时长(秒)
     */
    private Integer audioDuration;

    /**
     * 音频文件大小(字节)
     */
    private Long audioFileSize;

    /**
     * 场景标签
     */
    private String sceneTag;

    /**
     * 情感标签
     */
    private String emotionTag;

    /**
     * 播放次数
     */
    private Integer playCount;

    /**
     * 分享次数
     */
    private Integer shareCount;

    /**
     * 最后播放时间
     */
    private Date lastPlayTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}

