package com.yokior.knowledge.util;

import com.yokior.knowledge.config.MatcherConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 段落匹配算法工厂类
 * 根据配置选择使用TF-IDF或BM25算法
 */
@Component
public class MatcherFactory {

    private final MatcherConfig matcherConfig;

    @Autowired
    public MatcherFactory(MatcherConfig matcherConfig) {
        this.matcherConfig = matcherConfig;
    }

    /**
     * 根据配置选择匹配算法，匹配最相关的段落
     *
     * @param queryTerms 查询词条列表
     * @param paragraphs 段落列表
     * @return 按相关性排序的段落索引列表
     */
    public List<Integer> match(List<String> queryTerms, List<String> paragraphs) {
        if (matcherConfig.getType() == MatcherConfig.MatcherType.BM25) {
            return BM25Matcher.match(queryTerms, paragraphs);
        } else {
            return TfIdfMatcher.match(queryTerms, paragraphs);
        }
    }

    /**
     * 根据配置选择匹配算法，计算段落的权重
     *
     * @param paragraphs 段落列表
     * @return 每个段落的权重Map列表
     */
    public List<Map<String, Double>> calculateWeights(List<String> paragraphs) {
        if (matcherConfig.getType() == MatcherConfig.MatcherType.BM25) {
            return BM25Matcher.calculateBM25Weights(paragraphs);
        } else {
            return TfIdfMatcher.calculateTfIdfWeights(paragraphs);
        }
    }

    /**
     * 将权重Map转换为JSON字符串
     *
     * @param weights 权重Map
     * @return JSON字符串
     */
    public String weightsToJsonString(Map<String, Double> weights) {
        if (matcherConfig.getType() == MatcherConfig.MatcherType.BM25) {
            return BM25Matcher.weightsToJsonString(weights);
        } else {
            return TfIdfMatcher.weightsToJsonString(weights);
        }
    }

    /**
     * 获取当前使用的匹配算法类型
     *
     * @return 匹配算法类型
     */
    public String getMatcherType() {
        return matcherConfig.getType().name();
    }
} 