package com.zyb.backend.model.dto.voicemodel;

import com.zyb.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 声音模型查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VoiceModelQueryRequest extends PageRequest implements Serializable {

    /**
     * 模型ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 模型名称（模糊搜索）
     */
    private String modelName;

    /**
     * 训练状态: 0-待训练 1-训练中 2-成功 3-失败
     */
    private Integer trainingStatus;

    /**
     * 声音类型: male/female/child
     */
    private String voiceType;

    @Serial
    private static final long serialVersionUID = 1L;
}

