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
     * 网络工具重试（如声音克隆API）
     * 最多重试2次，延迟1秒
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String executeNetworkCall(String toolName, Callable<String> call) throws Exception {
        log.debug("执行网络调用: {}", toolName);
        return call.call();
    }

    /**
     * AI服务重试（如语音合成）
     * 最多重试2次，延迟1秒
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String executeAiService(String toolName, Callable<String> call) throws Exception {
        log.debug("执行AI服务: {}", toolName);
        return call.call();
    }

    /**
     * 文件操作重试
     * 最多重试1次，延迟500ms
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 2,
            backoff = @Backoff(delay = 500)
    )
    public String executeFileOperation(String toolName, Callable<String> operation) throws Exception {
        log.debug("执行文件操作: {}", toolName);
        return operation.call();
    }

    /**
     * 网络调用失败后的兜底方法
     */
    @Recover
    public String recoverNetworkCall(Exception e, String toolName, Callable<String> call) {
        log.error("网络调用最终失败: {}, 错误: {}", toolName, e.getMessage());
        return "网络调用失败: " + e.getMessage();
    }

    /**
     * AI服务失败后的兜底方法
     */
    @Recover
    public String recoverAiService(Exception e, String toolName, Callable<String> call) {
        log.error("AI服务调用最终失败: {}, 错误: {}", toolName, e.getMessage());
        return "AI服务调用失败: " + e.getMessage();
    }

    /**
     * 文件操作失败后的兜底方法
     */
    @Recover
    public String recoverFileOperation(Exception e, String toolName, Callable<String> operation) {
        log.error("文件操作最终失败: {}, 错误: {}", toolName, e.getMessage());
        return "文件操作失败: " + e.getMessage();
    }
}


