package com.zyb.backend.controller;

import com.zyb.backend.agent.VoiceKeeperAgent;
import com.zyb.backend.agent.service.AgentManagerService;
import com.zyb.backend.annotation.AuthCheck;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.constant.UserConstant;
import jakarta.annotation.Resource;
import org.apache.http.util.Asserts;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AgentManagerService agentManagerService;


    /**
     * 流式调用智能体
     */
    @GetMapping("/agent/chat")
    public SseEmitter doChatWithAgent(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {

        // 参数校验
        Asserts.notEmpty(message, String.valueOf(ResultCode.PARAMS_ERROR));

        // 生成会话ID
        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = agentManagerService.generateConversationId("agent");
        }

        // 获取或创建智能体实例
        VoiceKeeperAgent myManus = agentManagerService.getOrCreateAgent(conversationId);

        return myManus.runStream(message);
    }
}
