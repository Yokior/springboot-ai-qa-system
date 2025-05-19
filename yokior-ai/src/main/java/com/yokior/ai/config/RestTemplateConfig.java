package com.yokior.ai.config;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig
{
    @Bean
    public RestTemplate restTemplate()
    {
        // 创建HttpClient实例
        HttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(100)         // 最大连接数
                .setMaxConnPerRoute(10)       // 每个路由的最大连接数
                .build();

        // 创建HTTP客户端工厂
        HttpComponentsClientHttpRequestFactory factory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(30000);              // 连接超时时间（毫秒）
        factory.setReadTimeout(60000);                 // 读取超时时间（毫秒）
        factory.setConnectionRequestTimeout(30000);    // 连接请求超时时间（毫秒）
        
        // 创建RestTemplate，不使用BufferingClientHttpRequestFactory，以便支持流式响应
        return new RestTemplate(factory);
    }
}