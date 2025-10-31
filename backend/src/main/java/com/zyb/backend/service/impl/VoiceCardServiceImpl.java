package com.zyb.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyb.backend.mapper.VoiceCardMapper;
import com.zyb.backend.model.entity.VoiceCard;
import com.zyb.backend.service.VoiceCardService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 声音卡片服务实现
 * AI原生应用：提供纯粹的数据操作，不包含业务逻辑
 */
@Service
public class VoiceCardServiceImpl extends ServiceImpl<VoiceCardMapper, VoiceCard>
        implements VoiceCardService {

    @Override
    public List<VoiceCard> listByUserId(Long userId) {
        LambdaQueryWrapper<VoiceCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceCard::getUserId, userId)
                .eq(VoiceCard::getIsDelete, 0)
                .orderByDesc(VoiceCard::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<VoiceCard> listByUserIdAndScene(Long userId, String sceneTag) {
        LambdaQueryWrapper<VoiceCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceCard::getUserId, userId)
                .eq(VoiceCard::getSceneTag, sceneTag)
                .eq(VoiceCard::getIsDelete, 0)
                .orderByDesc(VoiceCard::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<VoiceCard> listByVoiceModelId(Long voiceModelId) {
        LambdaQueryWrapper<VoiceCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceCard::getVoiceModelId, voiceModelId)
                .eq(VoiceCard::getIsDelete, 0)
                .orderByDesc(VoiceCard::getCreateTime);
        return list(wrapper);
    }

    @Override
    public boolean increasePlayCount(Long cardId) {
        VoiceCard card = getById(cardId);
        if (card == null) {
            return false;
        }
        card.setPlayCount(card.getPlayCount() == null ? 1 : card.getPlayCount() + 1);
        card.setLastPlayTime(new Date());
        return updateById(card);
    }
}





