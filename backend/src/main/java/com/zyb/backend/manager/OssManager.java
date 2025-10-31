package com.zyb.backend.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 阿里云 OSS 操作
 */
@Component
public class OssManager {
    
    @Value("${ali-oss.bucketName}")
    private String bucketName;

    @Resource
    private OSS ossClient;

    /**
     * 上传对象
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
        return ossClient.putObject(putObjectRequest);
    }
} 