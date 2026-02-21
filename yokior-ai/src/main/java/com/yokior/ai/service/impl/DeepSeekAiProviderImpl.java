package com.yokior.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.service.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yokior
 * @description
 * @date 2025/5/19 14:40
 */
@Service("DeepSeek")
public class DeepSeekAiProviderImpl extends BaseAiProviderImpl
{
    public DeepSeekAiProviderImpl(RestTemplate restTemplate,
                                  @Value("${ai.providers.DeepSeek.apiKey}") String apiKey,
                                  @Value("${ai.providers.DeepSeek.model}") String model,
                                  @Value("${ai.providers.DeepSeek.endpoint}") String endpoint,
                                  @Value("${ai.providers.DeepSeek.temperature}") Double temperature,
                                  @Value("${ai.providers.DeepSeek.maxTokens:2048}") Integer maxTokens,
                                  @Value("${ai.providers.DeepSeek.stream}") Boolean stream)
    {
        super(restTemplate, apiKey, model, endpoint, temperature, maxTokens, stream);
    }

}
