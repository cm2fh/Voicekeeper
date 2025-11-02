package com.zyb.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置
 * 基于 Elasticsearch 实现语义检索
 */
@Configuration
@Slf4j
public class VectorStoreConfig {

    /**
     * 配置 Elasticsearch 向量存储
     */
    @Bean
    public ElasticsearchVectorStore elasticsearchVectorStore(
            RestClient restClient,
            EmbeddingModel embeddingModel
    ) {
        
        ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .build();
        
        log.info("Elasticsearch 向量存储初始化完成");
        return vectorStore;
    }
}

