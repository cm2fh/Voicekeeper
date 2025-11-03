package com.zyb.backend.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * 工具重试帮助类
 * 使用 Spring Retry 注解
 */
@Component
@Slf4j
public class ToolRetryHelper {

    /**
     * 网络工具重试
     * 最多重试2次，延迟1秒
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String executeNetworkCall(String toolName, Callable<String> call) throws Exception {
        log.info("执行网络调用重试: {}", toolName);
        return call.call();
    }

    /**
     * 网络调用失败后的兜底方法
     */
    @Recover
    public String recoverNetworkCall(Exception e, String toolName, Callable<String> call) {
        log.error("网络调用最终失败: {}, 错误: {}", toolName, e.getMessage());
        return "网络调用失败: " + e.getMessage();
    }
}


