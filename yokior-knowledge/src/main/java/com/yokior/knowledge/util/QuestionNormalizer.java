package com.yokior.knowledge.util;

import com.yokior.common.constant.CacheConstants;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

/**
 * 问题标准化工具类
 * 用于将相似问题转换为相同的标识，实现缓存命中
 *
 * @author yokior
 */
public class QuestionNormalizer
{
    /**
     * 问题标准化：分词、排序、拼接
     *
     * @param question 原始问题
     * @return 标准化后的问题字符串
     */
    public static String normalize(String question)
    {
        if (StringUtils.isEmpty(question))
        {
            return "";
        }

        // 分词并去停用词
        List<String> terms = HanLPProcessor.segment(question);
        if (terms.isEmpty())
        {
            return "";
        }

        // 排序关键词
        Collections.sort(terms);

        // 用下划线拼接
        return String.join("_", terms);
    }

    /**
     * 生成问题的MD5哈希值（取前16位）
     *
     * @param question 原始问题
     * @return 问题哈希值
     */
    public static String hash(String question)
    {
        String normalized = normalize(question);
        if (StringUtils.isEmpty(normalized))
        {
            return null;
        }

        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest)
            {
                sb.append(String.format("%02x", b & 0xff));
            }
            // 取前16位作为哈希值
            return sb.substring(0, 16);
        }
        catch (Exception e)
        {
            throw new RuntimeException("生成问题哈希失败", e);
        }
    }

    /**
     * 构建知识库检索缓存Key
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @return 缓存Key
     */
    public static String buildKnowledgeCacheKey(Long teamId, String questionHash)
    {
        return CacheConstants.QA_KNOWLEDGE_KEY + teamId + ":" + questionHash;
    }

    /**
     * 构建AI回答缓存Key
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @param model AI模型名称
     * @return 缓存Key
     */
    public static String buildAnswerCacheKey(Long teamId, String questionHash, String model)
    {
        return CacheConstants.QA_ANSWER_KEY + teamId + ":" + questionHash + ":" + model;
    }

    /**
     * 构建问题频率计数器Key
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @return 计数器Key
     */
    public static String buildFrequencyKey(Long teamId, String questionHash)
    {
        return CacheConstants.QA_FREQ_KEY + teamId + ":" + questionHash;
    }

    /**
     * 构建团队问答缓存Key前缀（用于批量删除）
     *
     * @param teamId 团队ID
     * @return 缓存Key前缀
     */
    public static String buildTeamCachePattern(Long teamId)
    {
        return "qa:*:" + teamId + ":*";
    }
}
