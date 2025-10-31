package com.zyb.backend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（脱敏）
 * 返回给前端的用户信息，不包含敏感字段
 */
@Data
public class UserVO implements Serializable {

    /**
     * ID
     */
    private Long id;

    /**
     * 用户编号
     */
    private String userNo;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 头像URL
     */
    private String avatar;

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

    /**
     * 注册时间
     */
    private Date registerTime;

    /**
     * 创建时间
     */
    private Date createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}