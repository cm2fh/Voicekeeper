package com.zyb.backend.common.exception;

import com.zyb.backend.common.response.ResultCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务异常类
 */
@Setter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    @Getter
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    public BusinessException(ResultCode ResultCode) {
        super(ResultCode.getMessage());
        this.code = ResultCode.getCode();
        this.message = ResultCode.getMessage();
    }

    public BusinessException(ResultCode ResultCode, String message) {
        super(message);
        this.code = ResultCode.getCode();
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}