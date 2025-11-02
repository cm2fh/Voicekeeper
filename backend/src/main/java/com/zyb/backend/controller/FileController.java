package com.zyb.backend.controller;

import cn.hutool.core.io.FileUtil;
import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.BaseResponse;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.manager.OssManager;
import com.zyb.backend.model.dto.file.UploadFileRequest;
import com.zyb.backend.model.entity.User;
import com.zyb.backend.model.enums.FileUploadBizEnum;
import com.zyb.backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;

/**
 * 文件接口
 * VoiceKeeper - 支持图片和音频文件上传
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;

    @Resource
    private OssManager ossManager;

    @Value("${aliyun.oss.url-prefix}")
    private String ossUrlPrefix;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                           UploadFileRequest uploadFileRequest, HttpServletRequest request) {
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);  // ✅ 不以 / 开头
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            ossManager.putObject(filepath, file);
            // 返回可访问地址
            return BaseResponse.success(ossUrlPrefix + filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = {}", filepath, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 校验文件
     * VoiceKeeper - 支持图片和音频文件验证
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            // 用户头像：最大1M，支持常见图片格式
            if (fileSize > ONE_M) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "头像文件大小不能超过 1M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "头像只支持 jpeg/jpg/png/webp/svg 格式");
            }
        } else if (FileUploadBizEnum.VOICE_SAMPLE.equals(fileUploadBizEnum)) {
            // 声音样本：最大50M，支持常见音频格式
            if (fileSize > 50 * ONE_M) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "声音样本文件大小不能超过 50M");
            }
            if (!Arrays.asList("mp3", "wav", "m4a", "ogg", "flac", "aac").contains(fileSuffix)) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "声音样本只支持 mp3/wav/m4a/ogg/flac/aac 格式");
            }
        } else if (FileUploadBizEnum.VOICE_CARD.equals(fileUploadBizEnum) || 
                   FileUploadBizEnum.VOICE_GENERATED.equals(fileUploadBizEnum)) {
            // 生成的语音：最大10M
            if (fileSize > 10 * ONE_M) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "语音文件大小不能超过 10M");
            }
            if (!Arrays.asList("mp3", "wav", "m4a").contains(fileSuffix)) {
                throw new BusinessException(ResultCode.PARAMS_ERROR, "语音文件只支持 mp3/wav/m4a 格式");
            }
        }
    }
}
