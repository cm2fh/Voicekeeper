package com.zyb.backend.controller;

import com.zyb.backend.agent.VoiceKeeperAgent;
import com.zyb.backend.agent.service.AgentManagerService;
import com.zyb.backend.annotation.AuthCheck;
import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.constant.UserConstant;
import com.zyb.backend.model.entity.User;
import com.zyb.backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    @Resource
    private AgentManagerService agentManagerService;

    @Resource
    private UserService userService;

    /**
     * 流式调用智能体
     */
    @GetMapping("/agent/chat")
    public SseEmitter doChatWithAgent(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId,
            HttpServletRequest request) {

        log.info("收到AI对话请求: message={}, conversationId={}", message, conversationId);

        // 验证登录
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }

        // 参数校验
        if (StringUtils.isBlank(message)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR, "消息不能为空");
        }

        // 生成会话ID
        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = agentManagerService.generateConversationId("agent");
        }

        log.info("开始处理AI对话: userId={}, conversationId={}", loginUser.getId(), conversationId);

        // 获取或创建智能体实例（传递userId）
        VoiceKeeperAgent myManus = agentManagerService.getOrCreateAgent(conversationId, loginUser.getId());

        return myManus.runStream(message);
    }
}
