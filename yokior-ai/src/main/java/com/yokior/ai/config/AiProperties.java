package com.yokior.ai.config;

import com.yokior.ai.domain.ProviderConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties
{

    private boolean enabled;
    private String provider; // 当前使用的 AI 服务
    private int timeout;
    private int retry;

    private Map<String, ProviderConfig> providers;


}