package com.yokior.knowledge.service.impl;

import com.yokior.common.core.redis.RedisCache;
import com.yokior.knowledge.config.QaCacheProperties;
import com.yokior.knowledge.domain.vo.KnowledgeMatchVO;
import com.yokior.knowledge.service.QaCacheService;
import com.yokior.knowledge.util.QuestionNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 问答缓存服务实现类
 *
 * @author yokior
 */
@Slf4j
@Service
public class QaCacheServiceImpl implements QaCacheService
{
    @Autowired
    private RedisCache redisCache;

    @Autowired
    private QaCacheProperties qaCacheProperties;

    @Override
    public long recordQuestionFrequency(Long teamId, String questionHash)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return 0;
        }

        try
        {
            String key = QuestionNormalizer.buildFrequencyKey(teamId, questionHash);
            Long count = redisCache.increment(key, 1,
                    qaCacheProperties.getFrequencyCounterDays(), TimeUnit.DAYS);
            log.debug("问题频率计数: teamId={}, hash={}, count={}", teamId, questionHash, count);
            return count == null ? 0 : count;
        }
        catch (Exception e)
        {
            log.error("记录问题频率失败: teamId={}, hash={}", teamId, questionHash, e);
            return 0;
        }
    }

    @Override
    public boolean isFrequentQuestion(Long teamId, String questionHash)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return false;
        }

        try
        {
            String key = QuestionNormalizer.buildFrequencyKey(teamId, questionHash);
            Object countObj = redisCache.getCacheObject(key);
            if (countObj == null)
            {
                return false;
            }
            long count = Long.parseLong(countObj.toString());
            return count >= qaCacheProperties.getFrequentThreshold();
        }
        catch (Exception e)
        {
            log.error("判断高频问题失败: teamId={}, hash={}", teamId, questionHash, e);
            return false;
        }
    }

    @Override
    public void cacheKnowledgeResult(Long teamId, String questionHash, List<KnowledgeMatchVO> knowledgeList)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return;
        }

        try
        {
            String key = QuestionNormalizer.buildKnowledgeCacheKey(teamId, questionHash);
            redisCache.setCacheObject(key, knowledgeList,
                    qaCacheProperties.getKnowledgeCacheHours(), TimeUnit.HOURS);
            log.debug("缓存知识库检索结果: teamId={}, hash={}, size={}",
                    teamId, questionHash, knowledgeList.size());
        }
        catch (Exception e)
        {
            log.error("缓存知识库检索结果失败: teamId={}, hash={}", teamId, questionHash, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<KnowledgeMatchVO> getCachedKnowledgeResult(Long teamId, String questionHash)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return null;
        }

        try
        {
            String key = QuestionNormalizer.buildKnowledgeCacheKey(teamId, questionHash);
            Object cached = redisCache.getCacheObject(key);
            if (cached instanceof List)
            {
                log.debug("命中知识库缓存: teamId={}, hash={}", teamId, questionHash);
                return (List<KnowledgeMatchVO>) cached;
            }
            return null;
        }
        catch (Exception e)
        {
            log.error("获取知识库缓存失败: teamId={}, hash={}", teamId, questionHash, e);
            return null;
        }
    }

    @Override
    public void cacheAiAnswer(Long teamId, String questionHash, String model, String answer)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return;
        }

        try
        {
            String key = QuestionNormalizer.buildAnswerCacheKey(teamId, questionHash, model);
            redisCache.setCacheObject(key, answer,
                    qaCacheProperties.getAnswerCacheHours(), TimeUnit.HOURS);
            log.debug("缓存AI回答: teamId={}, hash={}, model={}", teamId, questionHash, model);
        }
        catch (Exception e)
        {
            log.error("缓存AI回答失败: teamId={}, hash={}, model={}", teamId, questionHash, model, e);
        }
    }

    @Override
    public String getCachedAiAnswer(Long teamId, String questionHash, String model)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return null;
        }

        try
        {
            String key = QuestionNormalizer.buildAnswerCacheKey(teamId, questionHash, model);
            Object cached = redisCache.getCacheObject(key);
            if (cached instanceof String)
            {
                log.debug("命中AI回答缓存: teamId={}, hash={}, model={}", teamId, questionHash, model);
                return (String) cached;
            }
            return null;
        }
        catch (Exception e)
        {
            log.error("获取AI回答缓存失败: teamId={}, hash={}, model={}", teamId, questionHash, model, e);
            return null;
        }
    }

    @Override
    public long clearTeamQaCache(Long teamId)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return 0;
        }

        try
        {
            String pattern = QuestionNormalizer.buildTeamCachePattern(teamId);
            long count = redisCache.deleteByPattern(pattern);
            log.info("清除团队问答缓存: teamId={}, count={}", teamId, count);
            return count;
        }
        catch (Exception e)
        {
            log.error("清除团队缓存失败: teamId={}", teamId, e);
            return 0;
        }
    }

    @Override
    public boolean hasKnowledgeCache(Long teamId, String questionHash)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return false;
        }

        try
        {
            String key = QuestionNormalizer.buildKnowledgeCacheKey(teamId, questionHash);
            return Boolean.TRUE.equals(redisCache.hasKey(key));
        }
        catch (Exception e)
        {
            log.error("检查知识库缓存失败: teamId={}, hash={}", teamId, questionHash, e);
            return false;
        }
    }

    @Override
    public boolean hasAiAnswerCache(Long teamId, String questionHash, String model)
    {
        if (!qaCacheProperties.isEnabled())
        {
            return false;
        }

        try
        {
            String key = QuestionNormalizer.buildAnswerCacheKey(teamId, questionHash, model);
            return Boolean.TRUE.equals(redisCache.hasKey(key));
        }
        catch (Exception e)
        {
            log.error("检查AI回答缓存失败: teamId={}, hash={}, model={}", teamId, questionHash, model, e);
            return false;
        }
    }
}
