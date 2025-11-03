package com.zyb.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyb.backend.common.exception.BusinessException;
import com.zyb.backend.common.response.ResultCode;
import com.zyb.backend.model.entity.VoiceCard;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 声音卡片向量服务
 */
@Service
@Slf4j
public class VoiceCardVectorService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private VoiceCardService voiceCardService;

    /**
     * 创建卡片成功后索引单张卡片
     */
    public void indexCard(Long cardId) {
        try {
            // 1. 查询卡片数据
            VoiceCard card = voiceCardService.getById(cardId);
            if (card == null) {
                log.error("卡片不存在: cardId={}", cardId);
                return;
            }

            // 2. 组装文本
            String indexText = buildIndexText(card);
            log.info("索引文本: cardId={}, text={}", cardId, indexText);

            // 3. 准备 metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("cardId", card.getId());
            metadata.put("userId", card.getUserId());
            metadata.put("voiceModelId", card.getVoiceModelId());
            metadata.put("cardTitle", card.getCardTitle());
            metadata.put("sceneTag", card.getSceneTag() != null ? card.getSceneTag() : "custom");
            metadata.put("emotionTag", card.getEmotionTag() != null ? card.getEmotionTag() : "");
            metadata.put("createTime", card.getCreateTime().getTime());
            
            // 4. 创建 Document 对象
            Document document = new Document(cardId.toString(), indexText, metadata);

            // 5. 添加到向量存储
            vectorStore.add(List.of(document));

            log.info("卡片索引成功: cardId={}, title={}", cardId, card.getCardTitle());

        } catch (Exception e) {
            log.error("卡片索引失败: cardId={}, error={}", cardId, e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "卡片索引失败: " + e.getMessage());
        }
    }

    /**
     * 语义搜索卡片
     */
    public List<VoiceCard> semanticSearch(Long userId, String query, String sceneFilter, int topK) {
        try {
            log.info("语义搜索: userId={}, query={}, scene={}, topK={}", 
                    userId, query, sceneFilter, topK);

            // 1. 构建搜索请求
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK * 2)
                    .similarityThreshold(0.5)
                    .build();

            // 2. 执行向量检索
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("向量检索完成，找到 {} 个匹配结果", results.size());

            // 3. 应用层过滤
            List<Document> filteredResults = results.stream()
                    .filter(doc -> {
                        // 过滤 userId
                        Object userIdObj = doc.getMetadata().get("userId");
                        if (userIdObj == null) return false;
                        
                        Long docUserId;
                        if (userIdObj instanceof Integer) {
                            docUserId = ((Integer) userIdObj).longValue();
                        } else if (userIdObj instanceof Long) {
                            docUserId = (Long) userIdObj;
                        } else {
                            return false;
                        }
                        
                        if (!docUserId.equals(userId)) {
                            return false;
                        }
                        
                        // 过滤 sceneTag
                        if (sceneFilter != null && !sceneFilter.isEmpty()) {
                            Object sceneObj = doc.getMetadata().get("sceneTag");
                            return sceneFilter.equals(sceneObj);
                        }
                        
                        return true;
                    })
                    .limit(topK)
                    .toList();
            
            log.info("过滤后剩余 {} 个结果", filteredResults.size());

            // 4. 提取卡片ID
            List<Long> cardIds = filteredResults.stream()
                    .map(doc -> {
                        Object cardIdObj = doc.getMetadata().get("cardId");
                        if (cardIdObj == null) return null;
                        
                        if (cardIdObj instanceof Integer) {
                            return ((Integer) cardIdObj).longValue();
                        } else if (cardIdObj instanceof Long) {
                            return (Long) cardIdObj;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (cardIds.isEmpty()) {
                log.info("未找到匹配的卡片");
                return List.of();
            }

            // 5. 查询 MySQL 补充完整信息
            List<VoiceCard> cards = voiceCardService.listByIds(cardIds);
            
            // 6. 按原始相似度顺序排序
            Map<Long, VoiceCard> cardMap = cards.stream()
                    .collect(Collectors.toMap(VoiceCard::getId, card -> card));
            
            List<VoiceCard> sortedCards = cardIds.stream()
                    .map(cardMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("语义搜索完成，返回 {} 张卡片", sortedCards.size());
            return sortedCards;

        } catch (Exception e) {
            log.error("语义搜索失败: {}", query, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "语义搜索失败: " + e.getMessage());
        }
    }

    /**
     * 删除卡片索引
     */
    public void deleteCardIndex(Long cardId) {
        try {
            vectorStore.delete(List.of(cardId.toString()));
            log.info("卡片索引删除成功: cardId={}", cardId);
        } catch (Exception e) {
            log.error("删除卡片索引失败: cardId={}, error={}", cardId, e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "删除卡片索引失败: " + e.getMessage());
        }
    }

    /**
     * 批量索引所有卡片
     * 异步执行，用于初始化或重建索引
     * 
     * @param userId 用户ID，为null时索引所有用户的卡片
     */
    public void indexAllCards(Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始批量索引卡片: userId={}", userId);
                
                // 1. 查询所有需要索引的卡片
                List<VoiceCard> cards;
                if (userId != null) {
                    cards = voiceCardService.list(new LambdaQueryWrapper<VoiceCard>()
                            .eq(VoiceCard::getUserId, userId)
                            .eq(VoiceCard::getIsDelete, 0));
                } else {
                    cards = voiceCardService.list(new LambdaQueryWrapper<VoiceCard>()
                            .eq(VoiceCard::getIsDelete, 0));
                }
                
                log.info("共需索引 {} 张卡片", cards.size());
                
                // 2. 批量索引
                int successCount = 0;
                int failCount = 0;
                
                for (VoiceCard card : cards) {
                    try {
                        indexCard(card.getId());
                        successCount++;
                        
                        // 每10张卡片输出一次进度
                        if (successCount % 10 == 0) {
                            log.info("批量索引进度: {}/{}", successCount, cards.size());
                        }
                    } catch (Exception e) {
                        log.error("索引卡片失败: cardId={}, error={}", card.getId(), e.getMessage());
                        failCount++;
                    }
                }
                
                log.info("批量索引完成: 成功={}, 失败={}", successCount, failCount);
                
            } catch (Exception e) {
                log.error("批量索引失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 构建索引文本
     */
    private String buildIndexText(VoiceCard card) {
        StringBuilder text = new StringBuilder();

        // 1. 标题
        if (card.getCardTitle() != null && !card.getCardTitle().isEmpty()) {
            text.append("卡片标题：").append(card.getCardTitle()).append("\n");
        }

        // 2. 文字内容
        if (card.getTextContent() != null && !card.getTextContent().isEmpty()) {
            text.append("文字内容：").append(card.getTextContent()).append("\n");
        }

        // 3. 场景描述
        if (card.getSceneTag() != null && !card.getSceneTag().isEmpty()) {
            String sceneDesc = getSceneDescription(card.getSceneTag());
            text.append("使用场景：").append(sceneDesc).append("\n");
        }

        // 4. 情感标签
        if (card.getEmotionTag() != null && !card.getEmotionTag().isEmpty()) {
            String emotionDesc = getEmotionDescription(card.getEmotionTag());
            text.append("情感特征：").append(emotionDesc).append("\n");
        }
        
        // 5. 场景关联的情感关键词
        String emotionKeywords = getEmotionKeywords(card.getSceneTag());
        if (!emotionKeywords.isEmpty()) {
            text.append("情感关键词：").append(emotionKeywords).append("\n");
        }

        return text.toString().trim();
    }

    /**
     * 场景标签转描述文字
     */
    private String getSceneDescription(String sceneTag) {
        return switch (sceneTag) {
            case "morning" -> "早晨问候，充满活力和正能量，让人感到开心愉悦，适合清晨起床时播放，传递祝福和美好心情";
            case "night" -> "夜晚问候，温馨柔和，让人感到温暖安心，适合睡前播放，带来宁静和放松的感觉";
            case "encourage" -> "鼓励支持，充满力量和希望，让人感到被支持和理解，在困难或挫折时给予信心和勇气";
            case "miss" -> "表达思念，深情温柔，让人感到被牵挂和爱护，传递想念和期待";
            default -> "自定义";
        };
    }

    /**
     * 情感标签转描述文字
     */
    private String getEmotionDescription(String emotionTag) {
        return switch (emotionTag) {
            case "warm" -> "温暖关怀，让人感到被爱和温暖";
            case "gentle" -> "温柔轻声，柔和舒缓";
            case "energetic" -> "充满活力，积极向上，让人开心";
            case "sad" -> "深情感伤，温柔治愈";
            default -> "";
        };
    }
    
    /**
     * 根据场景生成情感关键词
     */
    private String getEmotionKeywords(String sceneTag) {
        if (sceneTag == null || sceneTag.isEmpty()) {
            return "温暖 陪伴 关心 温柔";
        }
        
        return switch (sceneTag) {
            case "morning" -> "开心 快乐 愉悦 活力 正能量 美好 祝福 元气 阳光 积极 充满希望";
            case "night" -> "温暖 安心 放松 宁静 柔和 舒适 安慰 平和 轻松 温馨";
            case "encourage" -> "支持 力量 信心 勇气 希望 理解 鼓励 坚强 相信 加油 振奋";
            case "miss" -> "温柔 思念 牵挂 爱 关心 温暖 深情 想念 陪伴 珍惜";
            default -> "温暖 陪伴 关心 温柔 爱 美好";
        };
    }
}

