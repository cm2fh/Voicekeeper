package com.zyb.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyb.backend.common.DeleteRequest;
import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.BaseResponse;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.model.dto.voicemodel.VoiceModelAddRequest;
import com.zyb.backend.model.dto.voicemodel.VoiceModelQueryRequest;
import com.zyb.backend.model.dto.voicemodel.VoiceModelUpdateRequest;
import com.zyb.backend.model.entity.User;
import com.zyb.backend.model.entity.VoiceModel;
import com.zyb.backend.model.vo.VoiceModelVO;
import com.zyb.backend.service.UserService;
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
 * 声音模型接口
 */
@RestController
@RequestMapping("/voicemodel")
@Slf4j
public class VoiceModelController {

    @Resource
    private VoiceModelService voiceModelService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建声音模型
     */
    @PostMapping("/add")
    public BaseResponse<Long> addVoiceModel(@Valid @RequestBody VoiceModelAddRequest addRequest,
                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        
        VoiceModel voiceModel = new VoiceModel();
        BeanUtils.copyProperties(addRequest, voiceModel);
        voiceModel.setUserId(loginUser.getId());
        
        // 设置默认值
        if (voiceModel.getTrainingStatus() == null) {
            voiceModel.setTrainingStatus(0);
        }
        if (voiceModel.getUseCount() == null) {
            voiceModel.setUseCount(0);
        }
        if (StringUtils.isBlank(voiceModel.getAiProvider())) {
            voiceModel.setAiProvider("dashscope");
        }
        
        boolean result = voiceModelService.save(voiceModel);
        ThrowUtils.throwIf(!result, ResultCode.FAILED);
        return BaseResponse.success(voiceModel.getId());
    }

    /**
     * 删除声音模型
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteVoiceModel(@RequestBody DeleteRequest deleteRequest,
                                                   HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        
        // 判断是否存在
        VoiceModel oldVoiceModel = voiceModelService.getById(id);
        ThrowUtils.throwIf(oldVoiceModel == null, ResultCode.NOT_FOUND);
        
        // 仅本人或管理员可删除
        if (!oldVoiceModel.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        boolean b = voiceModelService.removeById(id);
        return BaseResponse.success(b);
    }

    /**
     * 更新声音模型（仅本人）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateVoiceModel(@Valid @RequestBody VoiceModelUpdateRequest updateRequest,
                                                   HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = updateRequest.getId();
        
        // 判断是否存在
        VoiceModel oldVoiceModel = voiceModelService.getById(id);
        ThrowUtils.throwIf(oldVoiceModel == null, ResultCode.NOT_FOUND);
        
        // 仅本人可修改
        if (!oldVoiceModel.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        VoiceModel voiceModel = new VoiceModel();
        BeanUtils.copyProperties(updateRequest, voiceModel);
        
        boolean result = voiceModelService.updateById(voiceModel);
        ThrowUtils.throwIf(!result, ResultCode.FAILED);
        return BaseResponse.success(true);
    }

    /**
     * 根据 id 获取声音模型
     */
    @GetMapping("/get/vo")
    public BaseResponse<VoiceModelVO> getVoiceModelVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        VoiceModel voiceModel = voiceModelService.getById(id);
        if (voiceModel == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return BaseResponse.success(getVoiceModelVO(voiceModel));
    }

    /**
     * 分页获取当前用户创建的声音模型列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<VoiceModelVO>> listMyVoiceModelVOByPage(@RequestBody VoiceModelQueryRequest queryRequest,
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
        
        Page<VoiceModel> voiceModelPage = voiceModelService.page(
                new Page<>(current, size),
                getQueryWrapper(queryRequest)
        );
        
        Page<VoiceModelVO> voiceModelVOPage = new Page<>(current, size, voiceModelPage.getTotal());
        List<VoiceModelVO> voiceModelVOList = voiceModelPage.getRecords().stream()
                .map(this::getVoiceModelVO)
                .collect(Collectors.toList());
        voiceModelVOPage.setRecords(voiceModelVOList);
        
        return BaseResponse.success(voiceModelVOPage);
    }

    /**
     * 获取当前用户的所有声音模型（不分页）
     */
    @GetMapping("/my/list")
    public BaseResponse<List<VoiceModelVO>> listMyVoiceModels(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        
        List<VoiceModel> voiceModels = voiceModelService.listByUserId(loginUser.getId());
        
        List<VoiceModelVO> voiceModelVOList = voiceModels.stream()
                .map(this::getVoiceModelVO)
                .collect(Collectors.toList());
        
        return BaseResponse.success(voiceModelVOList);
    }

    // endregion

    /**
     * 获取查询包装类
     */
    private LambdaQueryWrapper<VoiceModel> getQueryWrapper(VoiceModelQueryRequest queryRequest) {
        LambdaQueryWrapper<VoiceModel> wrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return wrapper;
        }
        
        Long id = queryRequest.getId();
        Long userId = queryRequest.getUserId();
        String modelName = queryRequest.getModelName();
        Integer trainingStatus = queryRequest.getTrainingStatus();
        String voiceType = queryRequest.getVoiceType();
        
        wrapper.eq(id != null, VoiceModel::getId, id);
        wrapper.eq(userId != null, VoiceModel::getUserId, userId);
        wrapper.like(StringUtils.isNotBlank(modelName), VoiceModel::getModelName, modelName);
        wrapper.eq(trainingStatus != null, VoiceModel::getTrainingStatus, trainingStatus);
        wrapper.eq(StringUtils.isNotBlank(voiceType), VoiceModel::getVoiceType, voiceType);
        wrapper.eq(VoiceModel::getIsDelete, 0);
        
        // 排序
        wrapper.orderByDesc(VoiceModel::getCreateTime);
        
        return wrapper;
    }

    /**
     * 将实体转换为VO
     */
    private VoiceModelVO getVoiceModelVO(VoiceModel voiceModel) {
        if (voiceModel == null) {
            return null;
        }
        VoiceModelVO voiceModelVO = new VoiceModelVO();
        BeanUtils.copyProperties(voiceModel, voiceModelVO);
        return voiceModelVO;
    }
}

