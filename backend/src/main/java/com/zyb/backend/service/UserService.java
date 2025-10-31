package com.zyb.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyb.backend.model.dto.user.UserQueryRequest;
import com.zyb.backend.model.entity.User;
import com.zyb.backend.model.vo.LoginUserVO;
import com.zyb.backend.model.vo.UserVO;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param checkPassword 确认密码
     * @return 新用户 ID
     */
    long userRegister(String username, String password, String checkPassword);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param request HTTP请求
     * @return 登录用户信息
     */
    LoginUserVO userLogin(String username, String password, HttpServletRequest request);


    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
