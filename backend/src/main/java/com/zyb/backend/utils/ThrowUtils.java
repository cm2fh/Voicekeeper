package com.zyb.backend.utils;

import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.ResultCode;

/**
 * 抛异常工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     */
    public static void throwIf(boolean condition, ResultCode resultCode) {
        throwIf(condition, new BusinessException(resultCode));
    }

    /**
     * 条件成立则抛异常
     */
    public static void throwIf(boolean condition, ResultCode resultCode, String message) {
        throwIf(condition, new BusinessException(resultCode, message));
    }
}
