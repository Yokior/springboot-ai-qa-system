package com.yokior.ai.config;

import com.yokior.ai.service.AiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * AI 提供商工厂
 *
 * @author yokior
 */
@Component
public class AiProviderFactory
{

    @Autowired
    private ApplicationContext context;

    /**
     * 获取 AI 提供商
     *
     * @param providerName 提供商名称
     * @return AI 提供商
     */
    public AiProvider getProvider(String providerName)
    {
        Object bean = context.getBean(providerName);
        if (bean instanceof AiProvider)
        {
            return (AiProvider) bean;
        }
        throw new RuntimeException("不支持的 AI 提供商: " + providerName);
    }
}