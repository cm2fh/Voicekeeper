package com.zyb.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 声音卡片表
 * @TableName voice_card
 */
@TableName(value ="voice_card")
@Data
public class VoiceCard implements Serializable {
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
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
     * 场景: morning/night/encourage/miss/custom
     */
    private String sceneTag;

    /**
     * 情感标签: warm/gentle/energetic/sad
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

    /**
     * 是否删除
     */
    private Integer isDelete;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}