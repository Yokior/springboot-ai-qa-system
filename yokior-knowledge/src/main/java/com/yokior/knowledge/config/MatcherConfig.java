package com.yokior.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 段落匹配算法配置类
 */
@Configuration
@ConfigurationProperties(prefix = "document.matcher")
public class MatcherConfig {

    /**
     * 匹配算法类型：TF_IDF 或 BM25
     */
    private MatcherType type = MatcherType.BM25;

    /**
     * BM25参数k1: 控制词频缩放的参数，通常在1.2-2.0之间
     */
    private double k1 = 1.5;

    /**
     * BM25参数b: 控制文档长度归一化的参数，通常为0.75
     */
    private double b = 0.75;

    /**
     * 匹配算法类型枚举
     */
    public enum MatcherType {
        /**
         * TF-IDF算法
         */
        TF_IDF,
        
        /**
         * BM25算法
         */
        BM25
    }

    public MatcherType getType() {
        return type;
    }

    public void setType(MatcherType type) {
        this.type = type;
    }

    public double getK1() {
        return k1;
    }

    public void setK1(double k1) {
        this.k1 = k1;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }
} 