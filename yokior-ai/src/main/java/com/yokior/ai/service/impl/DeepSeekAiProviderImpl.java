package com.yokior.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.ProviderConfig;
import com.yokior.ai.service.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yokior
 * @description
 * @date 2025/5/19 14:40
 */
@Service("deepseek")
public class DeepSeekAiProviderImpl implements AiProvider
{
    @Value("${ai.providers.deepseek.apiKey}")
    private String apiKey;
    
    @Value("${ai.providers.deepseek.model}")
    private String model;
    
    @Value("${ai.providers.deepseek.endpoint}")
    private String endpoint;
    
    @Value("${ai.providers.deepseek.temperature}")
    private Double temperature;
    
    @Value("${ai.providers.deepseek.maxTokens:2000}")
    private Integer maxTokens;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekAiProviderImpl(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取模型回答
     *
     * @param prompt
     * @param history
     * @param options
     * @return
     */
    @Override
    public String getCompletion(String prompt, List<ChatMessage> history, Map<String, Object> options)
    {
        try
        {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();

            if (history != null)
            {
                for (ChatMessage msg : history)
                {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", msg.getRole());
                    m.put("content", msg.getContent());
                    messages.add(m);
                }
            }

            // 添加当前问题
            Map<String, String> currentMsg = new HashMap<>();
            currentMsg.put("role", "user");
            currentMsg.put("content", prompt);
            messages.add(currentMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            // 设置 Header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint, entity, String.class);

            // 返回模型的回答
            return parseResponse(response.getBody());

        }
        catch (Exception e)
        {
            throw new RuntimeException("调用 DeepSeek 失败: " + e.getMessage(), e);
        }
    }

    private String parseResponse(String responseBody) throws Exception
    {
        // 解析 JSON 响应
        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
        List<?> choices = (List<?>) map.get("choices");
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return (String) message.get("content");
    }
}
