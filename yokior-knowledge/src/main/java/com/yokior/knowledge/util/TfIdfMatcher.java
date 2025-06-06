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
 * TF-IDF匹配工具类
 */
public class TfIdfMatcher
{

    /**
     * 使用TF-IDF算法匹配最相关的段落
     *
     * @param queryTerms 查询词条列表
     * @param paragraphs 段落列表
     * @return 按相关性排序的段落索引列表
     */
    public static List<Integer> match(List<String> queryTerms, List<String> paragraphs)
    {
        List<Map<String, Integer>> termFreqList = new ArrayList<>();
        Map<String, Integer> docFreqMap = new HashMap<>();

        // 构建每个段落的词频表 & 统计 DF
        for (String para : paragraphs)
        {
            List<String> terms = HanLPProcessor.segment(para);
            Set<String> uniqueTerms = new HashSet<>(terms);
            Map<String, Integer> freqMap = new HashMap<>();

            for (String term : terms)
            {
                freqMap.put(term, freqMap.getOrDefault(term, 0) + 1);
            }

            termFreqList.add(freqMap);

            for (String term : uniqueTerms)
            {
                docFreqMap.put(term, docFreqMap.getOrDefault(term, 0) + 1);
            }
        }

        int totalDocs = paragraphs.size();
        List<Double> scores = new ArrayList<>();

        for (int i = 0; i < paragraphs.size(); i++)
        {
            double score = 0.0;
            Map<String, Integer> tfMap = termFreqList.get(i);

            for (String term : queryTerms)
            {
                int tf = tfMap.getOrDefault(term, 0);
                int df = docFreqMap.getOrDefault(term, 0);
                
                // 修改IDF计算方式，确保始终为正数
                // 使用 IDF = log(N/(df+1)) + 1 公式，与 calculateTfIdfWeights 保持一致
                double idf = Math.log((double) totalDocs / (df + 1)) + 1.0;
                
                // 确保IDF始终为正数
                if (idf < 0) {
                    idf = 0.01; // 设置一个小的正数作为最小值
                }
                
                double tfidf = tf * idf;
                score += tfidf;
            }

            scores.add(score);
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++)
        {
            indices.add(i);
        }

        // 排序取 Top-K
        return indices.stream()
                .sorted(Comparator.comparingDouble(scores::get).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 计算段落的TF-IDF权重
     *
     * @param paragraphs 段落列表
     * @return 每个段落的TF-IDF权重Map列表
     */
    public static List<Map<String, Double>> calculateTfIdfWeights(List<String> paragraphs)
    {
        List<List<String>> allTerms = new ArrayList<>();
        Map<String, Integer> docFreqMap = new HashMap<>();

        // 第一步：分词并计算文档频率
        for (String paragraph : paragraphs)
        {
            List<String> terms = HanLPProcessor.segment(paragraph);
            allTerms.add(terms);

            // 统计每个词在多少个文档中出现
            Set<String> uniqueTerms = new HashSet<>(terms);
            for (String term : uniqueTerms)
            {
                docFreqMap.put(term, docFreqMap.getOrDefault(term, 0) + 1);
            }
        }

        int totalDocs = paragraphs.size();
        List<Map<String, Double>> result = new ArrayList<>();

        // 第二步：计算每个段落中每个词的TF-IDF值
        for (List<String> terms : allTerms)
        {
            Map<String, Integer> termFreq = new HashMap<>();
            for (String term : terms)
            {
                termFreq.put(term, termFreq.getOrDefault(term, 0) + 1);
            }

            Map<String, Double> tfIdfMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : termFreq.entrySet())
            {
                String term = entry.getKey();
                int tf = entry.getValue();
                int df = docFreqMap.getOrDefault(term, 0);
                
                // 修改IDF计算方式，确保始终为正数
                // 使用 IDF = log(N/(df+1)) + 1 公式
                double idf = Math.log((double) totalDocs / (df + 1)) + 1.0;
                
                // 确保IDF始终为正数
                if (idf < 0) {
                    idf = 0.01; // 设置一个小的正数作为最小值
                }
                
                double tfIdf = tf * idf;

                tfIdfMap.put(term, tfIdf);
            }

            result.add(tfIdfMap);
        }

        return result;
    }

    /**
     * 将TF-IDF权重Map转换为JSON字符串
     *
     * @param weights TF-IDF权重Map
     * @return JSON字符串，格式为{"词1":0.5,"词2":0.3,...}
     */
    public static String weightsToJsonString(Map<String, Double> weights)
    {
        if (weights == null || weights.isEmpty())
        {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Double> entry : weights.entrySet())
        {
            if (!first)
            {
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