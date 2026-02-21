package com.yokior.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 问答缓存配置属性
 *
 * @author yokior
 */
@Component
@ConfigurationProperties(prefix = "qa.cache")
public class QaCacheProperties
{
    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 高频问题阈值（访问次数达到此值后缓存AI回答）
     */
    private int frequentThreshold = 5;

    /**
     * 知识库缓存时长（小时）
     */
    private int knowledgeCacheHours = 168;

    /**
     * AI回答缓存时长（小时）
     */
    private int answerCacheHours = 24;

    /**
     * 频率计数器过期天数
     */
    private int frequencyCounterDays = 30;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public int getFrequentThreshold()
    {
        return frequentThreshold;
    }

    public void setFrequentThreshold(int frequentThreshold)
    {
        this.frequentThreshold = frequentThreshold;
    }

    public int getKnowledgeCacheHours()
    {
        return knowledgeCacheHours;
    }

    public void setKnowledgeCacheHours(int knowledgeCacheHours)
    {
        this.knowledgeCacheHours = knowledgeCacheHours;
    }

    public int getAnswerCacheHours()
    {
        return answerCacheHours;
    }

    public void setAnswerCacheHours(int answerCacheHours)
    {
        this.answerCacheHours = answerCacheHours;
    }

    public int getFrequencyCounterDays()
    {
        return frequencyCounterDays;
    }

    public void setFrequencyCounterDays(int frequencyCounterDays)
    {
        this.frequencyCounterDays = frequencyCounterDays;
    }
}
