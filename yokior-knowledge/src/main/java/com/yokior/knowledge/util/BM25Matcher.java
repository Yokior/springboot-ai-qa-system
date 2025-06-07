package com.yokior.knowledge.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BM25匹配工具类
 * BM25是对TF-IDF的改进，增加了文档长度归一化和词频饱和控制
 */
public class BM25Matcher {

    /**
     * BM25参数k1: 控制词频缩放的参数，通常在1.2-2.0之间
     */
    private static final double K1 = 1.5;

    /**
     * BM25参数b: 控制文档长度归一化的参数，通常为0.75
     */
    private static final double B = 0.75;

    /**
     * 使用BM25算法匹配最相关的段落
     *
     * @param queryTerms 查询词条列表
     * @param paragraphs 段落列表
     * @return 按相关性排序的段落索引列表
     */
    public static List<Integer> match(List<String> queryTerms, List<String> paragraphs) {
        List<List<String>> allTerms = new ArrayList<>();
        Map<String, Integer> docFreqMap = new HashMap<>();
        List<Integer> docLengths = new ArrayList<>();
        
        // 第一步：分词并计算文档频率和文档长度
        for (String paragraph : paragraphs) {
            List<String> terms = HanLPProcessor.segment(paragraph);
            allTerms.add(terms);
            docLengths.add(terms.size());
            
            // 统计每个词在多少个文档中出现
            Set<String> uniqueTerms = new HashSet<>(terms);
            for (String term : uniqueTerms) {
                docFreqMap.put(term, docFreqMap.getOrDefault(term, 0) + 1);
            }
        }
        
        // 计算平均文档长度
        double avgDocLength = docLengths.stream().mapToInt(Integer::intValue).average().orElse(0);
        
        int totalDocs = paragraphs.size();
        List<Double> scores = new ArrayList<>();
        
        // 第二步：计算每个段落的BM25得分
        for (int i = 0; i < paragraphs.size(); i++) {
            double score = 0.0;
            List<String> docTerms = allTerms.get(i);
            int docLength = docLengths.get(i);
            
            // 统计当前文档的词频
            Map<String, Integer> termFreq = new HashMap<>();
            for (String term : docTerms) {
                termFreq.put(term, termFreq.getOrDefault(term, 0) + 1);
            }
            
            // 计算查询词的BM25得分
            for (String term : queryTerms) {
                int tf = termFreq.getOrDefault(term, 0);
                if (tf == 0) continue;
                
                int df = docFreqMap.getOrDefault(term, 0);
                
                // IDF计算：log((N-df+0.5)/(df+0.5))
                double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);
                
                // BM25公式：IDF * ((tf * (k1 + 1)) / (tf + k1 * (1 - b + b * docLength / avgDocLength)))
                double tfScore = (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * docLength / avgDocLength));
                
                score += idf * tfScore;
            }
            
            scores.add(score);
        }
        
        // 排序并返回索引
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            indices.add(i);
        }
        
        return indices.stream()
                .sorted(Comparator.comparingDouble(scores::get).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * 计算段落的BM25权重
     *
     * @param paragraphs 段落列表
     * @return 每个段落的BM25权重Map列表
     */
    public static List<Map<String, Double>> calculateBM25Weights(List<String> paragraphs) {
        List<List<String>> allTerms = new ArrayList<>();
        Map<String, Integer> docFreqMap = new HashMap<>();
        List<Integer> docLengths = new ArrayList<>();
        
        // 第一步：分词并计算文档频率和文档长度
        for (String paragraph : paragraphs) {
            List<String> terms = HanLPProcessor.segment(paragraph);
            allTerms.add(terms);
            docLengths.add(terms.size());
            
            // 统计每个词在多少个文档中出现
            Set<String> uniqueTerms = new HashSet<>(terms);
            for (String term : uniqueTerms) {
                docFreqMap.put(term, docFreqMap.getOrDefault(term, 0) + 1);
            }
        }
        
        // 计算平均文档长度
        double avgDocLength = docLengths.stream().mapToInt(Integer::intValue).average().orElse(0);
        
        int totalDocs = paragraphs.size();
        List<Map<String, Double>> result = new ArrayList<>();
        
        // 第二步：计算每个段落中每个词的BM25权重
        for (int i = 0; i < paragraphs.size(); i++) {
            List<String> docTerms = allTerms.get(i);
            int docLength = docLengths.get(i);
            
            // 统计当前文档的词频
            Map<String, Integer> termFreq = new HashMap<>();
            for (String term : docTerms) {
                termFreq.put(term, termFreq.getOrDefault(term, 0) + 1);
            }
            
            Map<String, Double> bm25Map = new HashMap<>();
            
            // 计算每个词的BM25权重
            for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
                String term = entry.getKey();
                int tf = entry.getValue();
                int df = docFreqMap.getOrDefault(term, 0);
                
                // IDF计算：log((N-df+0.5)/(df+0.5))
                double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);
                
                // BM25公式：IDF * ((tf * (k1 + 1)) / (tf + k1 * (1 - b + b * docLength / avgDocLength)))
                double tfScore = (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * docLength / avgDocLength));
                
                double bm25Score = idf * tfScore;
                bm25Map.put(term, bm25Score);
            }
            
            result.add(bm25Map);
        }
        
        return result;
    }
    
    /**
     * 将BM25权重Map转换为JSON字符串
     *
     * @param weights BM25权重Map
     * @return JSON字符串，格式为{"词1":0.5,"词2":0.3,...}
     */
    public static String weightsToJsonString(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("\"").append(entry.getKey().replace("\"", "\\\"")).append("\":")
                    .append(String.format("%.4f", entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }
} 