package com.zyb.backend.controller;

import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.BaseResponse;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.service.VectorStoreManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 向量存储管理接口
 */
@RestController
@RequestMapping("/vectorstore")
@Slf4j
@Tag(name = "向量存储管理", description = "Elasticsearch 索引管理接口")
public class VectorStoreController {

    @Resource
    private VectorStoreManagementService vectorStoreManagementService;

    @PostMapping("/index/create")
    public BaseResponse<String> createIndex() {
        try {
            boolean exists = vectorStoreManagementService.checkIndexExists();
            if (exists) {
                return BaseResponse.success("索引已存在");
            }
            vectorStoreManagementService.createIndex();
            return BaseResponse.success("索引创建成功");
        } catch (Exception e) {
            log.error("创建索引失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "创建索引失败: " + e.getMessage());
        }
    }

    @PostMapping("/index/delete")
    public BaseResponse<String> deleteIndex() {
        try {
            vectorStoreManagementService.deleteIndex();
            return BaseResponse.success("索引删除成功");
        } catch (Exception e) {
            log.error("删除索引失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "删除索引失败: " + e.getMessage());
        }
    }

    @PostMapping("/index/recreate")
    public BaseResponse<String> recreateIndex() {
        try {
            vectorStoreManagementService.recreateIndex();
            return BaseResponse.success("索引重建成功");
        } catch (Exception e) {
            log.error("重建索引失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "重建索引失败: " + e.getMessage());
        }
    }

    @PostMapping("/cards/reindex")
    public BaseResponse<String> reindexAllCards() {
        try {
            vectorStoreManagementService.reindexAllCards();
            return BaseResponse.success("批量索引已提交，正在后台执行");
        } catch (Exception e) {
            log.error("批量索引失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "批量索引失败: " + e.getMessage());
        }
    }

    @PostMapping("/index/check")
    public BaseResponse<Boolean> checkIndex() {
        try {
            boolean exists = vectorStoreManagementService.checkIndexExists();
            return BaseResponse.success(exists);
        } catch (Exception e) {
            log.error("检查索引失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "检查索引失败: " + e.getMessage());
        }
    }
}

