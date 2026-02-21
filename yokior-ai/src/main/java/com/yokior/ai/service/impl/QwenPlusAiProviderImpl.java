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
@Service("QwenPlus")
public class QwenPlusAiProviderImpl extends BaseAiProviderImpl {

    public QwenPlusAiProviderImpl(RestTemplate restTemplate,
                                  @Value("${ai.providers.QwenPlus.apiKey}") String apiKey,
                                  @Value("${ai.providers.QwenPlus.model}") String model,
                                  @Value("${ai.providers.QwenPlus.endpoint}") String endpoint,
                                  @Value("${ai.providers.QwenPlus.temperature}") Double temperature,
                                  @Value("${ai.providers.QwenPlus.maxTokens:2048}") Integer maxTokens,
                                  @Value("${ai.providers.QwenPlus.stream}") Boolean stream) {
        super(restTemplate, apiKey, model, endpoint, temperature, maxTokens, stream);
    }


}
