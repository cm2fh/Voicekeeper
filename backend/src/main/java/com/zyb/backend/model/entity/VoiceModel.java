package com.zyb.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 声音模型表
 * @TableName voice_model
 */
@TableName(value ="voice_model")
@Data
public class VoiceModel implements Serializable {
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
     * 模型名称(如:妈妈的声音)
     */
    private String modelName;

    /**
     * 声音描述
     */
    private String voiceDesc;

    /**
     * 样本音频OSS地址
     */
    private String sampleAudioUrl;

    /**
     * 样本时长(秒)
     */
    private Integer sampleDuration;

    /**
     * 样本文件大小(字节)
     */
    private Long sampleFileSize;

    /**
     * 第三方AI模型ID(ElevenLabs/阿里云)
     */
    private String aiModelId;

    /**
     * AI服务提供商
     */
    private String aiProvider;

    /**
     * 训练状态: 0-待训练 1-训练中 2-成功 3-失败
     */
    private Integer trainingStatus;

    /**
     * 训练消息(成功/失败原因)
     */
    private String trainingMessage;

    /**
     * 音质评分(0-5)
     */
    private BigDecimal qualityScore;

    /**
     * 声音类型: male/female/child
     */
    private String voiceType;

    /**
     * 使用次数
     */
    private Integer useCount;

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