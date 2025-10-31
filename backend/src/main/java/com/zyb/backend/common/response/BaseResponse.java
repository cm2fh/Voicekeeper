package com.zyb.backend.common.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用响应封装类
 */
@Data
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 是否成功
     */
    private Boolean success;

    public BaseResponse(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    /**
     * 成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, true);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(ResultCode.SUCCESS.getCode(), message, data, true);
    }

    /**
     * 失败响应
     */
    public static <T> BaseResponse<T> error(Integer code, String message) {
        return new BaseResponse<>(code, message, null, false);
    }

    /**
     * 失败响应（使用预定义错误码和自定义消息）
     */
    public static <T> BaseResponse<T> error(ResultCode resultCode, String message) {
        return error(resultCode.getCode(), message);
    }

}