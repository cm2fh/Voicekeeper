package com.zyb.backend.tools;

import jakarta.annotation.Resource;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具注册配置
 */
@Configuration
public class ToolRegistration {

    @Resource
    private FileTool fileTool;

    @Resource
    private SearchCardTool searchCardTool;

    @Resource
    private SearchVoiceModelTool searchVoiceModelTool;

    @Resource
    private VoiceCloneTool voiceCloneTool;

    @Resource
    private VoiceSynthesisTool voiceSynthesisTool;

    @Resource
    private SemanticSearchTool semanticSearchTool;

    @Resource
    private VoiceCardCreateTool voiceCardCreateTool;

    /**
     * 注册所有工具供 VoiceKeeperAgent 使用
     */
    @Bean("voiceTools")
    public ToolCallback[] voiceTools() {
        // 通用工具
        DateTimeTool dateTimeTool = new DateTimeTool();
        TerminateTool terminateTool = new TerminateTool();

        return ToolCallbacks.from(
                searchCardTool,
                searchVoiceModelTool,
                voiceCloneTool,
                voiceSynthesisTool,
                voiceCardCreateTool,
                semanticSearchTool,
                fileTool,
                dateTimeTool,
                terminateTool
        );
    }
}

