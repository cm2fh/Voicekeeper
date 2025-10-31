package com.zyb.backend.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端配置类
 */
@Configuration
public class OkHttpClientConfig {

    /**
     * 配置OkHttpClient
     * 用于调用外部AI服务，需要较长的超时时间
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.MINUTES)  // 连接超时3分钟
                .readTimeout(3, TimeUnit.MINUTES)     // 读取超时3分钟
                .writeTimeout(3, TimeUnit.MINUTES)    // 写入超时3分钟
                .retryOnConnectionFailure(true)       // 连接失败自动重试
                .build();
    }

    /**
     * 使用HTTP Client创建RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMinutes(3));
        return new RestTemplate(requestFactory);
    }

    /**
     * 使用HTTP Client创建RestClient.Builder
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMinutes(3));
        return RestClient.builder().requestFactory(requestFactory);
    }
}

