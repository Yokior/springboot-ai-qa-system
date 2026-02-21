package com.yokior.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.service.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * @author Yokior
 * @description
 * @date 2025/6/7 17:22
 */
@Service("Qwen3_235B_A22B")
public class Qwen3_235B_A22BAiProviderImpl extends BaseAiProviderImpl {
    public Qwen3_235B_A22BAiProviderImpl(RestTemplate restTemplate,
                                         @Value("${ai.providers.Qwen3_235B_A22B.apiKey}") String apiKey,
                                         @Value("${ai.providers.Qwen3_235B_A22B.model}") String model,
                                         @Value("${ai.providers.Qwen3_235B_A22B.endpoint}") String endpoint,
                                         @Value("${ai.providers.Qwen3_235B_A22B.temperature}") Double temperature,
                                         @Value("${ai.providers.Qwen3_235B_A22B.maxTokens:2048}") Integer maxTokens,
                                         @Value("${ai.providers.Qwen3_235B_A22B.stream}") Boolean stream) {
        super(restTemplate, apiKey, model, endpoint, temperature, maxTokens, stream);
    }

}