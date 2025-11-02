package com.zyb.backend.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 向量存储管理服务
 * 负责 Elasticsearch 索引的创建、删除和检查
 */
@Service
@Slf4j
public class VectorStoreManagementService {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.elasticsearch.initialize-schema}")
    private boolean initializeSchema;

    /**
     * 应用启动时检查并创建索引
     */
    @PostConstruct
    public void initIndex() {
        if (!initializeSchema) {
            log.info("跳过索引初始化（initialize-schema=false）");
            return;
        }

        try {
            boolean exists = checkIndexExists();
            if (!exists) {
                log.info("索引 {} 不存在，开始创建...", indexName);
                createIndex();
                log.info("索引 {} 创建成功", indexName);
            } else {
                log.info("索引 {} 已存在", indexName);
            }
        } catch (Exception e) {
            log.error("索引初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，允许应用继续启动
        }
    }

    /**
     * 检查索引是否存在
     */
    public boolean checkIndexExists() {
        try {
            ExistsRequest request = ExistsRequest.of(b -> b.index(indexName));
            return elasticsearchClient.indices().exists(request).value();
        } catch (Exception e) {
            log.error("检查索引存在性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建索引
     */
    public void createIndex() {
        try {
            CreateIndexRequest request = CreateIndexRequest.of(b -> b
                    .index(indexName)
                    .mappings(TypeMapping.of(m -> m
                            .properties("embedding", Property.of(p -> p
                                    .denseVector(dv -> dv
                                            .dims(dimensions)
                                            .similarity("cosine")
                                    )
                            ))
                            .properties("content", Property.of(p -> p.text(t -> t)))
                            .properties("metadata", Property.of(p -> p.object(o -> o
                                    .properties("cardId", Property.of(mp -> mp.long_(l -> l)))
                                    .properties("userId", Property.of(mp -> mp.long_(l -> l)))
                                    .properties("voiceModelId", Property.of(mp -> mp.long_(l -> l)))
                                    .properties("cardTitle", Property.of(mp -> mp.text(t -> t)))
                                    .properties("sceneTag", Property.of(mp -> mp.keyword(k -> k)))
                                    .properties("emotionTag", Property.of(mp -> mp.keyword(k -> k)))
                                    .properties("createTime", Property.of(mp -> mp.long_(l -> l)))
                            )))
                    ))
            );

            elasticsearchClient.indices().create(request);
            log.info("成功创建索引: {}", indexName);
        } catch (Exception e) {
            log.error("创建索引失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建索引失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() {
        try {
            boolean exists = checkIndexExists();
            if (exists) {
                log.info("开始删除索引: {}...", indexName);
                elasticsearchClient.indices().delete(d -> d.index(indexName));
                log.info("索引 {} 删除成功", indexName);
            } else {
                log.info("索引 {} 不存在，无需删除", indexName);
            }
        } catch (Exception e) {
            log.error("删除索引 {} 失败: {}", indexName, e.getMessage(), e);
            throw new RuntimeException("删除索引失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重建索引
     */
    public void recreateIndex() {
        try {
            log.info("开始重建索引: {}", indexName);
            deleteIndex();
            createIndex();
            log.info("索引 {} 重建成功", indexName);
        } catch (Exception e) {
            log.error("重建索引失败: {}", e.getMessage(), e);
            throw new RuntimeException("重建索引失败: " + e.getMessage(), e);
        }
    }
}

