package com.zyb.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyb.backend.mapper.VoiceModelMapper;
import com.zyb.backend.model.entity.VoiceModel;
import com.zyb.backend.service.VoiceModelService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 张云博
* @description 针对表【voice_model(声音模型表)】的数据库操作Service实现
* @createDate 2025-10-31 12:00:06
*/
@Service
public class VoiceModelServiceImpl extends ServiceImpl<VoiceModelMapper, VoiceModel>
        implements VoiceModelService {

    @Override
    public List<VoiceModel> listByUserId(Long userId) {
        LambdaQueryWrapper<VoiceModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceModel::getUserId, userId)
                .orderByDesc(VoiceModel::getCreateTime);
        return list(wrapper);
    }

    @Override
    public VoiceModel getByUserIdAndName(Long userId, String modelName) {
        LambdaQueryWrapper<VoiceModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceModel::getUserId, userId)
                .like(VoiceModel::getModelName, modelName);
        return getOne(wrapper);
    }

    @Override
    public boolean updateTrainingStatus(Long modelId, Integer status, String message) {
        LambdaUpdateWrapper<VoiceModel> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(VoiceModel::getId, modelId)
                .set(VoiceModel::getTrainingStatus, status)
                .set(VoiceModel::getTrainingMessage, message);
        return update(wrapper);
    }

    @Override
    public boolean increaseUseCount(Long modelId) {
        VoiceModel model = getById(modelId);
        if (model == null) {
            return false;
        }
        model.setUseCount(model.getUseCount() == null ? 1 : model.getUseCount() + 1);
        return updateById(model);
    }
}





