package com.zyb.backend.tools;

import com.zyb.backend.model.entity.VoiceCard;
import com.zyb.backend.service.VoiceCardVectorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 语义搜索工具
 * 让 Agent 具备长期记忆和语义检索能力
 */
@Component
@Slf4j
public class SemanticSearchTool {

    @Resource
    private VoiceCardVectorService vectorService;

    @Tool(description = """
        【语义搜索】模糊查询声音卡片
        使用时机：
        - 用户描述模糊："温柔的卡片"、"适合睡前的"
        - searchUserCards 未找到时的补充搜索
        不要用于：
        - 明确场景 → 直接用 searchUserCards
        
        重要：sceneFilter 使用规则
        - 用户明确提到场景（晚安、早安、鼓励、想念）→ 传对应标签
        - 用户只描述情感/特征（温暖、感动、柔和）→ 传 null（搜索所有场景）
        示例：
        - "找温暖的卡片" → sceneFilter=null
        - "找晚安卡片" → sceneFilter="night"
        """)
    public String searchCardsBySemantic(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "自然语言查询，描述想找的卡片特征") String query,
            @ToolParam(description = "场景过滤（可选，不确定时传null）：morning/night/encourage/miss/custom/null") String sceneFilter,
            @ToolParam(description = "返回数量，默认5") Integer topK
    ) {
        try {
            log.info("调用语义搜索: userId={}, query={}", userId, query);

            // 1. 参数处理
            int k = (topK != null && topK > 0) ? topK : 5;
            
            // 2. 调用向量检索
            List<VoiceCard> cards = vectorService.semanticSearch(
                    userId, query, sceneFilter, k
            );

            // 3. 格式化返回结果
            return formatSearchResults(cards, query);

        } catch (Exception e) {
            log.error("语义搜索失败: userId={}, query={}, error={}", userId, query, e.getMessage(), e);
            return formatErrorResult(e.getMessage());
        }
    }

    /**
     * 格式化搜索结果
     */
    private String formatSearchResults(List<VoiceCard> cards, String query) {
        if (cards.isEmpty()) {
            return buildEmptyResult(query);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("找到 %d 张匹配的卡片：\n\n", cards.size()));

        for (int i = 0; i < cards.size(); i ++) {
            VoiceCard card = cards.get(i);
            result.append(formatCard(i + 1, card));
        }

        // 添加友好提示
        result.append("\n提示：\n");
        result.append("- 可以说「播放第1张」来播放卡片\n");
        result.append("- 想查看更多可以说「再找5张」\n");

        return result.toString();
    }

    /**
     * 格式化单张卡片
     */
    private String formatCard(int index, VoiceCard card) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        return String.format(
                """
                %d. 【%s】
                   - 卡片ID: %d
                   - 场景: %s
                   - 内容: %s
                   - 创建时间: %s
                """,
                index,
                card.getCardTitle(),
                card.getId(),
                getSceneText(card.getSceneTag()),
                truncateText(card.getTextContent()),
                dateFormat.format(card.getCreateTime())
        );
    }

    /**
     * 空结果提示
     */
    private String buildEmptyResult(String query) {
        return String.format(
                """
                未找到与「%s」匹配的卡片。
                建议：
                1. 尝试更通用的描述（如：温柔的 → 晚安相关）
                2. 检查是否已创建过相关卡片
                3. 可以说「创建一张xxx卡片」来新建
                """,
                query
        );
    }

    /**
     * 错误结果提示
     */
    private String formatErrorResult(String errorMessage) {
        return String.format(
                """
                搜索遇到问题：%s
                可能原因：
                1. 向量存储服务未启动
                2. 还没有索引过卡片
                建议稍后重试。
                """,
                errorMessage
        );
    }

    /**
     * 场景标签转中文
     */
    private String getSceneText(String sceneTag) {
        if (sceneTag == null) {
            return "自定义";
        }

        return switch (sceneTag) {
            case "morning" -> "早安问候";
            case "night" -> "晚安问候";
            case "encourage" -> "鼓励支持";
            case "miss" -> "表达思念";
            default -> "自定义";
        };
    }

    /**
     * 截断文本
     */
    private String truncateText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= 50) {
            return text;
        }
        return text.substring(0, 50) + "...";
    }
}

