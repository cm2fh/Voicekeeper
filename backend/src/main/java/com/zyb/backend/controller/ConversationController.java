package com.zyb.backend.controller;

import com.zyb.backend.agent.service.AgentManagerService;
import com.zyb.backend.common.response.BaseResponse;
import com.zyb.backend.common.response.ResultCode;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Resource
    private AgentManagerService agentManagerService;

    /**
     * 清理会话数据
     */
    @GetMapping("/clear")
    public BaseResponse<String> clearConversation(@RequestParam String conversationId) {
        try {
            agentManagerService.clearConversation(conversationId);
            return BaseResponse.success("会话数据已清理");
        } catch (Exception e) {
            return BaseResponse.error(ResultCode.FAILED, "清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话信息
     */
    @GetMapping("/info")
    public BaseResponse<String> getConversationInfo(@RequestParam String conversationId) {
        try {
            boolean exists = agentManagerService.conversationExists(conversationId);
            int messageCount = agentManagerService.getMessageCount(conversationId);

            String info = String.format("会话存在: %s, 消息数量: %d", exists, messageCount);
            return BaseResponse.success(info);
        } catch (Exception e) {
            return BaseResponse.error(ResultCode.FAILED, "获取信息失败: " + e.getMessage());
        }
    }
}
