package com.zyb.backend.tools;

import cn.hutool.core.io.FileUtil;
import com.zyb.backend.constant.FileConstant;
import com.zyb.backend.utils.ToolRetryHelper;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 文件工具
 */
@Component
public class FileTool {

    private final String FILE_DIR = FileConstant.VOICE_DIR + "file";

    @Resource
    private ToolRetryHelper retryHelper;

    @Tool(description = "读取文件内容")
    public String readFile(@ToolParam(description = "文件名") String fileName) {
        String filePath = FILE_DIR + "/" + fileName;
        try {
            return retryHelper.executeFileOperation("readFile", () -> 
                FileUtil.readUtf8String(filePath)
            );
        } catch (Exception e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "写入文件内容")
    public String writeFile(
            @ToolParam(description = "文件名") String fileName,
            @ToolParam(description = "文件内容") String content) {
        String filePath = FILE_DIR + "/" + fileName;
        try {
            return retryHelper.executeFileOperation("writeFile", () -> {
                FileUtil.mkdir(FILE_DIR);
                FileUtil.writeUtf8String(content, filePath);
                return "文件写入成功: " + filePath;
            });
        } catch (Exception e) {
            return "写入文件失败: " + e.getMessage();
        }
    }
}

