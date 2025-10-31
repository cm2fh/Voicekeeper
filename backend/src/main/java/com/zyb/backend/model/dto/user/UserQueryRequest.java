package com.zyb.backend.model.dto.user;

import com.zyb.backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    
    /**
     * ID
     */
    private Long id;

    /**
     * 用户编号
     */
    private String userNo;

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 真实姓名（模糊查询）
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 角色：user-普通用户，admin-管理员
     */
    private String role;

    /**
     * 状态：0-禁用，1-正常
     */
    private Integer status;

    @Serial
    private static final long serialVersionUID = 1L;
}