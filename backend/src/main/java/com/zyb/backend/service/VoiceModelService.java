package com.zyb.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyb.backend.model.entity.VoiceModel;

import java.util.List;

/**
* @author 张云博
* @description 针对表【voice_model(声音模型表)】的数据库操作Service
* @createDate 2025-10-31 12:00:06
*/
public interface VoiceModelService extends IService<VoiceModel> {

    /**
     * 根据用户ID查询声音模型列表
     */
    List<VoiceModel> listByUserId(Long userId);

    /**
     * 根据用户ID和模型名称查询
     */
    VoiceModel getByUserIdAndName(Long userId, String modelName);

    /**
     * 更新训练状态
     */
    boolean updateTrainingStatus(Long modelId, Integer status, String message);

    /**
     * 增加使用次数
     */
    boolean increaseUseCount(Long modelId);
}
