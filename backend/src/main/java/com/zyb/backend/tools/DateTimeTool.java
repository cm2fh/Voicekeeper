package com.zyb.backend.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具
 */
@Component
public class DateTimeTool {

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 获取当前日期时间（默认格式）
     */
    @Tool(description = "获取当前日期时间（格式：yyyy-MM-dd HH:mm:ss）")
    public String getCurrentDateTime() {
        return getCurrentDateTimeWithFormat(DEFAULT_DATETIME_FORMAT);
    }

    /**
     * 获取当前日期时间（指定格式）
     */
    @Tool(description = "获取当前日期时间（自定义格式）")
    public String getCurrentDateTimeWithFormat(
            @ToolParam(description = "日期时间格式（如 yyyy-MM-dd HH:mm:ss）") String format
    ) {
        try {
            String formatPattern = (format == null || format.isEmpty()) ? DEFAULT_DATETIME_FORMAT : format;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
            return LocalDateTime.now().format(formatter);
        } catch (Exception e) {
            return "获取当前日期和时间时发生错误: " + e.getMessage();
        }
    }

    /**
     * 获取当前小时（用于场景判断）
     */
    @Tool(description = "获取当前小时（0-23），用于判断是早上、下午还是晚上")
    public int getCurrentHour() {
        return LocalDateTime.now().getHour();
    }
}

