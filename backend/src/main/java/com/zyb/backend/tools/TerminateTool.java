package com.zyb.backend.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 终止工具
 */
@Component
public class TerminateTool {

    @Tool(description = """
            当任务完成或无法继续时，调用此工具终止执行。
            使用场景：
            1. 已成功完成用户的所有请求
            2. 遇到无法解决的错误
            3. 需要用户提供更多信息才能继续
            """)
    public String doTerminate() {
        return "任务结束";
    }
}

