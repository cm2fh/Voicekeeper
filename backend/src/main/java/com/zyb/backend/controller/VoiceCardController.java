package com.zyb.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyb.backend.common.DeleteRequest;
import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.BaseResponse;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.model.dto.voicecard.VoiceCardAddRequest;
import com.zyb.backend.model.dto.voicecard.VoiceCardQueryRequest;
import com.zyb.backend.model.dto.voicecard.VoiceCardUpdateRequest;
import com.zyb.backend.model.entity.User;
import com.zyb.backend.model.entity.VoiceCard;
import com.zyb.backend.model.entity.VoiceModel;
import com.zyb.backend.model.vo.VoiceCardVO;
import com.zyb.backend.service.UserService;
import com.zyb.backend.service.VoiceCardService;
import com.zyb.backend.service.VoiceModelService;
import com.zyb.backend.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 声音卡片接口
 */
@RestController
@RequestMapping("/voicecard")
@Slf4j
public class VoiceCardController {

    @Resource
    private VoiceCardService voiceCardService;

    @Resource
    private UserService userService;

    @Resource
    private VoiceModelService voiceModelService;

    // region 增删改查

    /**
     * 创建声音卡片
     */
    @PostMapping("/add")
    public BaseResponse<Long> addVoiceCard(@Valid @RequestBody VoiceCardAddRequest addRequest,
                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        
        VoiceCard voiceCard = new VoiceCard();
        BeanUtils.copyProperties(addRequest, voiceCard);
        voiceCard.setUserId(loginUser.getId());
        
        // 设置默认值
        if (voiceCard.getPlayCount() == null) {
            voiceCard.setPlayCount(0);
        }
        if (voiceCard.getShareCount() == null) {
            voiceCard.setShareCount(0);
        }
        if (voiceCard.getAiGenerated() == null) {
            voiceCard.setAiGenerated(0);
        }
        
        boolean result = voiceCardService.save(voiceCard);
        ThrowUtils.throwIf(!result, ResultCode.FAILED);
        return BaseResponse.success(voiceCard.getId());
    }

    /**
     * 删除声音卡片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteVoiceCard(@RequestBody DeleteRequest deleteRequest,
                                                  HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        
        // 判断是否存在
        VoiceCard oldVoiceCard = voiceCardService.getById(id);
        ThrowUtils.throwIf(oldVoiceCard == null, ResultCode.NOT_FOUND);
        
        // 仅本人或管理员可删除
        if (!oldVoiceCard.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        boolean b = voiceCardService.removeById(id);
        return BaseResponse.success(b);
    }

    /**
     * 更新声音卡片（仅本人）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateVoiceCard(@Valid @RequestBody VoiceCardUpdateRequest updateRequest,
                                                  HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = updateRequest.getId();
        
        // 判断是否存在
        VoiceCard oldVoiceCard = voiceCardService.getById(id);
        ThrowUtils.throwIf(oldVoiceCard == null, ResultCode.NOT_FOUND);
        
        // 仅本人可修改
        if (!oldVoiceCard.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        VoiceCard voiceCard = new VoiceCard();
        BeanUtils.copyProperties(updateRequest, voiceCard);
        
        boolean result = voiceCardService.updateById(voiceCard);
        ThrowUtils.throwIf(!result, ResultCode.FAILED);
        return BaseResponse.success(true);
    }

    /**
     * 根据 id 获取声音卡片（脱敏）
     */
    @GetMapping("/get/vo")
    public BaseResponse<VoiceCardVO> getVoiceCardVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        VoiceCard voiceCard = voiceCardService.getById(id);
        if (voiceCard == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return BaseResponse.success(getVoiceCardVO(voiceCard));
    }

    /**
     * 分页获取当前用户创建的声音卡片列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<VoiceCardVO>> listMyVoiceCardVOByPage(@RequestBody VoiceCardQueryRequest queryRequest,
                                                                    HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        queryRequest.setUserId(loginUser.getId());
        
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ResultCode.PARAMS_ERROR);
        
        Page<VoiceCard> voiceCardPage = voiceCardService.page(
                new Page<>(current, size),
                getQueryWrapper(queryRequest)
        );
        
        Page<VoiceCardVO> voiceCardVOPage = new Page<>(current, size, voiceCardPage.getTotal());
        List<VoiceCardVO> voiceCardVOList = voiceCardPage.getRecords().stream()
                .map(this::getVoiceCardVO)
                .collect(Collectors.toList());
        voiceCardVOPage.setRecords(voiceCardVOList);
        
        return BaseResponse.success(voiceCardVOPage);
    }

    /**
     * 获取当前用户的所有声音卡片（不分页，用于卡片库）
     */
    @GetMapping("/my/list")
    public BaseResponse<List<VoiceCardVO>> listMyVoiceCards(HttpServletRequest request,
                                                             @RequestParam(required = false) String sceneTag) {
        User loginUser = userService.getLoginUser(request);
        
        List<VoiceCard> voiceCards;
        if (StringUtils.isNotBlank(sceneTag)) {
            voiceCards = voiceCardService.listByUserIdAndScene(loginUser.getId(), sceneTag);
        } else {
            voiceCards = voiceCardService.listByUserId(loginUser.getId());
        }
        
        List<VoiceCardVO> voiceCardVOList = voiceCards.stream()
                .map(this::getVoiceCardVO)
                .collect(Collectors.toList());
        
        return BaseResponse.success(voiceCardVOList);
    }

    /**
     * 增加播放次数
     */
    @PostMapping("/play/{id}")
    public BaseResponse<Boolean> increasePlayCount(@PathVariable Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        
        VoiceCard voiceCard = voiceCardService.getById(id);
        ThrowUtils.throwIf(voiceCard == null, ResultCode.NOT_FOUND);
        
        boolean result = voiceCardService.increasePlayCount(id);
        return BaseResponse.success(result);
    }

    // endregion

    /**
     * 获取查询包装类
     */
    private LambdaQueryWrapper<VoiceCard> getQueryWrapper(VoiceCardQueryRequest queryRequest) {
        LambdaQueryWrapper<VoiceCard> wrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return wrapper;
        }
        
        Long id = queryRequest.getId();
        Long userId = queryRequest.getUserId();
        Long voiceModelId = queryRequest.getVoiceModelId();
        String cardTitle = queryRequest.getCardTitle();
        String sceneTag = queryRequest.getSceneTag();
        String emotionTag = queryRequest.getEmotionTag();
        Integer aiGenerated = queryRequest.getAiGenerated();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        
        wrapper.eq(id != null, VoiceCard::getId, id);
        wrapper.eq(userId != null, VoiceCard::getUserId, userId);
        wrapper.eq(voiceModelId != null, VoiceCard::getVoiceModelId, voiceModelId);
        wrapper.like(StringUtils.isNotBlank(cardTitle), VoiceCard::getCardTitle, cardTitle);
        wrapper.eq(StringUtils.isNotBlank(sceneTag), VoiceCard::getSceneTag, sceneTag);
        wrapper.eq(StringUtils.isNotBlank(emotionTag), VoiceCard::getEmotionTag, emotionTag);
        wrapper.eq(aiGenerated != null, VoiceCard::getAiGenerated, aiGenerated);
        wrapper.eq(VoiceCard::getIsDelete, 0);
        
        // 动态排序
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            
            // 根据排序字段设置排序规则
            switch (sortField) {
                case "playCount":
                    wrapper.orderBy(true, isAsc, VoiceCard::getPlayCount);
                    break;
                case "createTime":
                    wrapper.orderBy(true, isAsc, VoiceCard::getCreateTime);
                    break;
                case "shareCount":
                    wrapper.orderBy(true, isAsc, VoiceCard::getShareCount);
                    break;
                case "audioDuration":
                    wrapper.orderBy(true, isAsc, VoiceCard::getAudioDuration);
                    break;
                default:
                    // 默认按创建时间倒序
                    wrapper.orderByDesc(VoiceCard::getCreateTime);
            }
        } else {
            // 没有指定排序字段时，默认按创建时间倒序
            wrapper.orderByDesc(VoiceCard::getCreateTime);
        }
        
        return wrapper;
    }

    /**
     * 将实体转换为VO
     */
    private VoiceCardVO getVoiceCardVO(VoiceCard voiceCard) {
        if (voiceCard == null) {
            return null;
        }
        VoiceCardVO voiceCardVO = new VoiceCardVO();
        BeanUtils.copyProperties(voiceCard, voiceCardVO);
        
        // 关联查询声音模型名称
        if (voiceCard.getVoiceModelId() != null) {
            VoiceModel voiceModel = voiceModelService.getById(voiceCard.getVoiceModelId());
            if (voiceModel != null) {
                voiceCardVO.setVoiceModelName(voiceModel.getModelName());
            }
        }
        
        return voiceCardVO;
    }
}

