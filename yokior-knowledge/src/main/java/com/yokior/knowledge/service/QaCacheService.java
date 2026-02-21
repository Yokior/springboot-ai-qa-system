package com.yokior.knowledge.service;

import com.yokior.knowledge.domain.vo.KnowledgeMatchVO;

import java.util.List;

/**
 * 问答缓存服务接口
 *
 * @author yokior
 */
public interface QaCacheService
{
    /**
     * 记录问题访问频率
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @return 当前访问次数
     */
    long recordQuestionFrequency(Long teamId, String questionHash);

    /**
     * 判断是否为高频问题
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @return 是否高频
     */
    boolean isFrequentQuestion(Long teamId, String questionHash);

    /**
     * 缓存知识库检索结果
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @param knowledgeList 知识匹配结果
     */
    void cacheKnowledgeResult(Long teamId, String questionHash, List<KnowledgeMatchVO> knowledgeList);

    /**
     * 获取缓存的知识库检索结果
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @return 知识匹配结果，不存在返回null
     */
    List<KnowledgeMatchVO> getCachedKnowledgeResult(Long teamId, String questionHash);

    /**
     * 缓存AI回答
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @param model AI模型名称
     * @param answer AI回答内容
     */
    void cacheAiAnswer(Long teamId, String questionHash, String model, String answer);

    /**
     * 获取缓存的AI回答
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @param model AI模型名称
     * @return AI回答内容，不存在返回null
     */
    String getCachedAiAnswer(Long teamId, String questionHash, String model);

    /**
     * 清除团队所有问答缓存
     *
     * @param teamId 团队ID
     * @return 清除的缓存数量
     */
    long clearTeamQaCache(Long teamId);

    /**
     * 检查知识库缓存是否存在
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @return 是否存在
     */
    boolean hasKnowledgeCache(Long teamId, String questionHash);

    /**
     * 检查AI回答缓存是否存在
     *
     * @param teamId 团队ID
     * @param questionHash 问题哈希值
     * @param model AI模型名称
     * @return 是否存在
     */
    boolean hasAiAnswerCache(Long teamId, String questionHash, String model);
}
