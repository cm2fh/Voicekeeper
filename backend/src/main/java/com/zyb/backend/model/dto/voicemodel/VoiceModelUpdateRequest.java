package com.zyb.backend.model.dto.voicemodel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 声音模型更新请求
 */
@Data
public class VoiceModelUpdateRequest implements Serializable {

    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空")
    private Long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 声音描述
     */
    private String voiceDesc;

    /**
     * 声音类型
     */
    private String voiceType;

    @Serial
    private static final long serialVersionUID = 1L;
}

