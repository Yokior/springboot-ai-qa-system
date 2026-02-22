package com.yokior.ai.service.impl;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.ChatSession;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;
import com.yokior.ai.domain.vo.ChatSessionVO;
import com.yokior.ai.mapper.ChatMessageMapper;
import com.yokior.ai.mapper.ChatSessionMapper;
import com.yokior.ai.service.AiChatService;
import com.yokior.ai.service.AiProvider;
import com.yokior.common.exception.ServiceException;
import com.yokior.knowledge.domain.vo.KnowledgeMatchVO;
import com.yokior.knowledge.service.IQaDocumentService;
import com.yokior.knowledge.service.QaCacheService;
import com.yokior.knowledge.util.QuestionNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService
{

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private IQaDocumentService qaDocumentService;

    @Autowired
    private QaCacheService qaCacheService;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${ai.defaultProvider:DeepSeek}")
    private String defaultProvider;

    @Value("${ai.maxParagraphsPerDoc:10}")
    private Integer maxParagraphsPerDoc;

    private final String separator = "【用户问题】";

    private final String systemPrompt = "以下是相关知识库内容，请根据这些内容回答用户的问题。如果相关内容中没有包含答案，请如实说明无法回答\n\n";

    @Override
    public String createSession(Long userId)
    {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setCreateTime(new Date());
        session.setUpdateTime(new Date());

        chatSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public ChatResponse processChat(ChatRequest request, Long userId)
    {
        return null;
    }

    @Override
    public List<ChatMessage> getChatHistory(String sessionId, Long userId)
    {
        // 验证会话归属
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId()))
        {
            throw new ServiceException("会话不存在或无权访问");
        }

        // 获取该会话的所有消息
        return chatMessageMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<ChatMessage> getChatHistory(String sessionId, Long userId, int count)
    {
        // 验证会话归属
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId()))
        {
            throw new ServiceException("会话不存在或无权访问");
        }

        // 获取该会话的指定数量的消息
        return chatMessageMapper.selectRecentBySessionId(sessionId, count);
    }

    @Override
    public void saveChatMessage(ChatMessage message)
    {
        if (message == null || StringUtils.isEmpty(message.getSessionId()))
        {
            throw new ServiceException("消息或会话ID不能为空");
        }

        chatMessageMapper.insert(message);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId)
    {
        // 验证会话归属
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId()))
        {
            throw new ServiceException("会话不存在或无权访问");
        }

        // 删除会话相关的所有消息
        chatMessageMapper.deleteBySessionId(sessionId);

        // 删除会话
        chatSessionMapper.deleteById(sessionId);
    }

    @Override
    public List<ChatSessionVO> getSessionVoListByUserId(Long userId)
    {
        List<ChatSessionVO> sessionVoList = chatSessionMapper.getSessionVoListByUserId(userId);

        return sessionVoList;
    }

    @Override
    public Boolean setSessionTitle(String sessionId, String title)
    {
        int update = chatSessionMapper.updateTitle(sessionId, title);

        return update > 0;
    }

    @Override
    @Transactional
    public Boolean clearChatHistory(String sessionId, Long userId)
    {
        // 验证会话归属
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId()))
        {
            throw new ServiceException("会话不存在或无权访问");
        }

        int delete = chatMessageMapper.deleteBySessionId(sessionId);

        int update = chatSessionMapper.updateTitle(sessionId, "新会话");

        return delete > 0 && update > 0;
    }

    @Override
    public void processStreamChat(ChatRequest request, Long userId, HttpServletResponse response) throws Exception
    {
        // 设置响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        log.info("收到流式聊天请求: prompt={}, sessionId={}",
                request.getPrompt(), request.getSessionId());

        // 消息内容收集器
        final StringBuilder aiResponseContent = new StringBuilder();

        try
        {
            // 获取或创建会话ID
            String sessionId = request.getSessionId();
            if (StringUtils.isEmpty(sessionId))
            {
                sessionId = createSession(userId);
                log.debug("已创建新会话: {}", sessionId);
            }

            final String finalSessionId = sessionId;

            // 获取聊天历史
            List<ChatMessage> history = getChatHistory(finalSessionId, userId, 10);
            log.debug("获取到历史消息: {} 条", history.size());

            String originalPrompt = request.getPrompt();
            StringBuilder prompt = new StringBuilder(originalPrompt);

            // 如果是初次会话 设置会话的标题
            if (history.isEmpty())
            {
                // 取prompt前10个字符作为会话标题
                String title = prompt.substring(0, Math.min(10, prompt.length()));
                // 如果有更多 加...
                if (prompt.length() > 10)
                {
                    title += "...";
                }
                setSessionTitle(finalSessionId, title);
            }

            // 获取用户选项
            Map<String, Object> options = request.getOptions();
            Long teamId = null;
            String questionHash = null;
            List<KnowledgeMatchVO> knowledgeMatchVOList = null;
            boolean isFrequentQuestion = false;

            // 处理知识库相关逻辑
            if (options != null && options.containsKey("teamId") && !StringUtils.isEmpty(options.get("teamId").toString()))
            {
                teamId = Long.parseLong(options.get("teamId").toString());

                // 生成问题哈希
                questionHash = QuestionNormalizer.hash(originalPrompt);
                log.debug("问题哈希: {}", questionHash);

                if (questionHash != null)
                {
                    // 记录问题频率
                    long freq = qaCacheService.recordQuestionFrequency(teamId, questionHash);
                    log.debug("问题频率: {}", freq);

                    // 尝试获取知识库缓存
                    knowledgeMatchVOList = qaCacheService.getCachedKnowledgeResult(teamId, questionHash);
                    if (knowledgeMatchVOList != null)
                    {
                        log.debug("命中知识库缓存");
                    }
                }

                // 如果缓存未命中，执行知识库检索
                if (knowledgeMatchVOList == null)
                {
                    knowledgeMatchVOList = qaDocumentService.searchKnowledge(teamId, originalPrompt, maxParagraphsPerDoc);

                    // 缓存知识库检索结果
                    if (questionHash != null)
                    {
                        qaCacheService.cacheKnowledgeResult(teamId, questionHash, knowledgeMatchVOList);
                        log.debug("缓存知识库检索结果");
                    }
                }

                // 处理知识匹配结果
                if (knowledgeMatchVOList != null && !knowledgeMatchVOList.isEmpty())
                {
                    log.debug("获取到知识匹配结果: {} 条", knowledgeMatchVOList.size());

                    String knowledgeContent = knowledgeMatchVOList.stream()
                            .map(k -> "\n[" + k.getFilename() + "]第[" + k.getParagraphOrder() + "]段\n" + k.getContent())
                            .collect(Collectors.joining("\n"));
                    prompt.append(systemPrompt);

                    prompt.append(knowledgeContent).append("\n").append(separator).append("\n").append(originalPrompt);
                }
                else
                {
                    log.debug("发送内容:\n{}\n未匹配到知识库内容", originalPrompt);
                }

                // 高频问题缓存检查
                isFrequentQuestion = questionHash != null && qaCacheService.isFrequentQuestion(teamId, questionHash);
            }

            // 保存用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setId(UUID.randomUUID().toString());
            userMessage.setSessionId(finalSessionId);
            userMessage.setContent(originalPrompt);
            userMessage.setRole("user");
            userMessage.setCreateTime(new Date());
            saveChatMessage(userMessage);
            log.debug("已保存用户消息: {}", userMessage.getId());

            // 包装响应输出流
            final CopyOutputStream copyOutputStream = new CopyOutputStream(response.getOutputStream(), aiResponseContent);
            log.debug("准备调用AI流式接口");

            // 根据用户选项选择AI提供者
            AiProvider selectedProvider = getAiProvider(options);
            String modelName = selectedProvider.getClass().getSimpleName();
            log.info("选择的AI模型: {}", modelName);

            String cachedAnswer = null;

            if (isFrequentQuestion)
            {
                log.debug("检测到高频问题，尝试获取AI回答缓存");
                cachedAnswer = qaCacheService.getCachedAiAnswer(teamId, questionHash, modelName);
            }

            if (cachedAnswer != null)
            {
                // 命中AI回答缓存，模拟流式输出
                log.info("命中AI回答缓存，直接返回缓存内容");
                simulateStreamFromCache(cachedAnswer, response.getOutputStream());
                aiResponseContent.append(cachedAnswer);
            }
            else
            {
                log.debug("发送AI内容: {}", prompt);
                // 调用AI流式API
                selectedProvider.streamCompletion(
                        prompt.toString(),
                        history,
                        options,
                        copyOutputStream
                );

                // 处理完成后缓存AI回答（仅高频问题）
                if (aiResponseContent.length() > 0 && isFrequentQuestion)
                {
                    String answer = aiResponseContent.toString();
                    qaCacheService.cacheAiAnswer(teamId, questionHash, modelName, answer);
                    log.debug("已缓存高频问题的AI回答");
                }
            }

            // 发送结束标记
            String endMessage = "data: [DONE]\n\n";
            response.getOutputStream().write(endMessage.getBytes());
            response.getOutputStream().flush();
            log.debug("已发送结束标记");

            // 处理完成后，保存AI回复
            if (aiResponseContent.length() > 0)
            {
                String finalContent = aiResponseContent.toString();
                log.debug("收集到的AI回复内容长度: {} 字符", finalContent.length());

                ChatMessage aiMessage = new ChatMessage();
                aiMessage.setId(UUID.randomUUID().toString());
                aiMessage.setSessionId(finalSessionId);
                aiMessage.setContent(finalContent);
                aiMessage.setRole("system");
                aiMessage.setCreateTime(new Date());
                saveChatMessage(aiMessage);

                log.debug("保存AI回复到数据库, 长度: {} 字符", finalContent.length());
            }
            else
            {
                log.warn("AI回复内容为空，未保存到数据库");
            }
        }
        catch (Exception e)
        {
            log.error("AI流式聊天处理异常", e);
            try
            {
                // 以SSE格式发送错误信息
                String errorMessage = "data: {\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}\n\n";
                response.getOutputStream().write(errorMessage.getBytes());
                response.getOutputStream().flush();

                // 发送结束标记
                String endMessage = "data: [DONE]\n\n";
                response.getOutputStream().write(endMessage.getBytes());
                response.getOutputStream().flush();
                log.debug("已发送错误信息和结束标记");
            }
            catch (Exception ex)
            {
                log.error("发送错误信息时发生异常", ex);
            }
        }
    }

    @Override
    public String getAiProviderName(Map<String, Object> options)
    {
        String providerName = defaultProvider;

        if (options != null && options.containsKey("model") && options.get("model") != null)
        {
            providerName = options.get("model").toString();
            log.debug("用户指定AI模型: {}", providerName);
        }
        else
        {
            log.debug("使用默认AI模型: {}", providerName);
        }

        return providerName;
    }

    @Override
    public KnowledgePromptResult buildKnowledgePrompt(String originalPrompt, Long teamId)
    {
        KnowledgePromptResult result = new KnowledgePromptResult();
        result.setPrompt(originalPrompt);

        if (teamId == null)
        {
            return result;
        }

        String questionHash = QuestionNormalizer.hash(originalPrompt);
        result.setQuestionHash(questionHash);

        if (questionHash != null)
        {
            // 记录问题频率
            qaCacheService.recordQuestionFrequency(teamId, questionHash);

            // 尝试获取知识库缓存
            List<KnowledgeMatchVO> knowledgeMatchVOList = qaCacheService.getCachedKnowledgeResult(teamId, questionHash);

            if (knowledgeMatchVOList == null)
            {
                // 缓存未命中，执行知识库检索
                knowledgeMatchVOList = qaDocumentService.searchKnowledge(teamId, originalPrompt, maxParagraphsPerDoc);
                qaCacheService.cacheKnowledgeResult(teamId, questionHash, knowledgeMatchVOList);
            }

            result.setKnowledgeMatches(knowledgeMatchVOList);
            result.setFrequentQuestion(qaCacheService.isFrequentQuestion(teamId, questionHash));

            // 构建增强prompt
            if (knowledgeMatchVOList != null && !knowledgeMatchVOList.isEmpty())
            {
                String knowledgeContent = knowledgeMatchVOList.stream()
                        .map(k -> "\n[" + k.getFilename() + "]第[" + k.getParagraphOrder() + "]段\n" + k.getContent())
                        .collect(Collectors.joining("\n"));

                StringBuilder prompt = new StringBuilder();
                prompt.append(systemPrompt)
                        .append(knowledgeContent)
                        .append("\n").append(separator).append("\n")
                        .append(originalPrompt);

                result.setPrompt(prompt.toString());
            }
        }

        return result;
    }

    /**
     * 根据用户选项获取AI提供者
     */
    private AiProvider getAiProvider(Map<String, Object> options)
    {
        String providerName = getAiProviderName(options);

        try
        {
            return applicationContext.getBean(providerName, AiProvider.class);
        }
        catch (Exception e)
        {
            log.warn("获取指定AI模型失败: {}，将使用默认模型: {}", providerName, defaultProvider);
            return applicationContext.getBean(defaultProvider, AiProvider.class);
        }
    }

    /**
     * 模拟流式输出缓存内容
     */
    private void simulateStreamFromCache(String cachedContent, OutputStream outputStream) throws IOException
    {
        int chunkSize = 20;
        ObjectMapper objectMapper = new ObjectMapper();

        for (int i = 0; i < cachedContent.length(); i += chunkSize)
        {
            int end = Math.min(i + chunkSize, cachedContent.length());
            String chunk = cachedContent.substring(i, end);

            Map<String, String> data = new HashMap<>();
            data.put("content", chunk);
            String jsonData = objectMapper.writeValueAsString(data);
            String sseMessage = "data: " + jsonData + "\n\n";

            outputStream.write(sseMessage.getBytes("UTF-8"));
            outputStream.flush();

            try
            {
                Thread.sleep(20);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 同时写入输出流和收集内容的输出流
     */
    private static class CopyOutputStream extends OutputStream
    {
        private final OutputStream target;
        private final StringBuilder collector;
        private StringBuilder lineBuffer = new StringBuilder();
        private final ObjectMapper objectMapper = new ObjectMapper();

        public CopyOutputStream(OutputStream target, StringBuilder collector)
        {
            this.target = target;
            this.collector = collector;
        }

        @Override
        public void write(int b) throws IOException
        {
            target.write(b);
            char c = (char) b;

            lineBuffer.append(c);

            if (c == '\n')
            {
                processLine(lineBuffer.toString());
                lineBuffer = new StringBuilder();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            target.write(b, off, len);
            String content = new String(b, off, len);

            for (int i = 0; i < content.length(); i++)
            {
                char c = content.charAt(i);
                lineBuffer.append(c);

                if (c == '\n')
                {
                    processLine(lineBuffer.toString());
                    lineBuffer = new StringBuilder();
                }
            }
        }

        private void processLine(String line)
        {
            line = line.trim();

            if (line.startsWith("data:"))
            {
                String content = line.substring(5).trim();

                if (!content.equals("[DONE]"))
                {
                    try
                    {
                        Map<String, Object> contentMap = objectMapper.readValue(content, Map.class);
                        if (contentMap.containsKey("content"))
                        {
                            String textContent = (String) contentMap.get("content");
                            collector.append(textContent);
                        }
                        else if (contentMap.containsKey("error"))
                        {
                            collector.append("[错误] ").append(contentMap.get("error"));
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("无法解析内容为JSON: {}", content);
                        collector.append(content);
                    }
                }
            }
        }

        @Override
        public void flush() throws IOException
        {
            target.flush();
        }

        @Override
        public void close() throws IOException
        {
            if (lineBuffer.length() > 0)
            {
                processLine(lineBuffer.toString());
            }
            target.close();
        }
    }
}
