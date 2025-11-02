package com.zyb.backend.model.dto.voicemodel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 声音模型创建请求
 */
@Data
public class VoiceModelAddRequest implements Serializable {

    /**
     * 模型名称(如:妈妈的声音)
     */
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    /**
     * 声音描述
     */
    private String voiceDesc;

    /**
     * 样本音频OSS地址
     */
    @NotBlank(message = "音频文件不能为空")
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
     * 声音类型: male/female/child
     */
    private String voiceType;

    @Serial
    private static final long serialVersionUID = 1L;
}

