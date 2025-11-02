package com.zyb.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyb.backend.annotation.AuthCheck;
import com.zyb.backend.common.DeleteRequest;
import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.BaseResponse;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.constant.UserConstant;
import com.zyb.backend.model.dto.user.*;
import com.zyb.backend.model.entity.User;
import com.zyb.backend.model.vo.LoginUserVO;
import com.zyb.backend.model.vo.UserVO;
import com.zyb.backend.service.UserService;
import com.zyb.backend.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    // region 登录相关

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        String username = userRegisterRequest.getUsername();
        String password = userRegisterRequest.getPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        long result = userService.userRegister(username, password, checkPassword);
        return BaseResponse.success(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        String username = userLoginRequest.getUsername();
        String password = userLoginRequest.getPassword();
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(username, password, request);
        return BaseResponse.success(loginUserVO);
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return BaseResponse.success(result);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return BaseResponse.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户（管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        
        // 如果未提供密码，使用默认密码
        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword("123456");  // 默认密码
        }
        
        // 生成用户编号（如果未提供）
        if (StringUtils.isBlank(user.getUserNo())) {
            user.setUserNo(generateUserNo());
        }
        
        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1);  // 默认正常状态
        }
        if (StringUtils.isBlank(user.getRole())) {
            user.setRole("user");  // 默认普通用户
        }
        
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ResultCode.FAILED);
        return BaseResponse.success(user.getId());
    }

    /**
     * 生成用户编号
     */
    private String generateUserNo() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 900) + 100;
        return "U" + (timestamp % 1000000) + random;
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return BaseResponse.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ResultCode.FAILED);
        return BaseResponse.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ResultCode.NOT_FOUND);
        return BaseResponse.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return BaseResponse.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return BaseResponse.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ResultCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return BaseResponse.success(userVOPage);
    }

    // endregion

    /**
     * 更新个人信息
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
            HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ResultCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ResultCode.FAILED);
        return BaseResponse.success(true);
    }
}
