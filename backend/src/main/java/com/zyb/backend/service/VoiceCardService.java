package com.zyb.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyb.backend.model.entity.VoiceCard;

import java.util.List;

/**
 * 声音卡片服务
 * AI原生应用：纯数据操作，业务逻辑由AI Agent决策
 */
public interface VoiceCardService extends IService<VoiceCard> {

    /**
     * 根据用户ID查询卡片列表
     */
    List<VoiceCard> listByUserId(Long userId);

    /**
     * 根据用户ID和场景标签查询
     */
    List<VoiceCard> listByUserIdAndScene(Long userId, String sceneTag);

    /**
     * 根据声音模型ID查询卡片
     */
    List<VoiceCard> listByVoiceModelId(Long voiceModelId);

    /**
     * 增加播放次数
     */
    boolean increasePlayCount(Long cardId);
}
