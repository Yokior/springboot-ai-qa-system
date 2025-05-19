package com.yokior.ai.service.impl;

import cn.hutool.core.lang.UUID;
import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.ChatSession;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;
import com.yokior.ai.mapper.ChatMessageMapper;
import com.yokior.ai.mapper.ChatSessionMapper;
import com.yokior.ai.service.AiChatService;
import com.yokior.ai.service.AiProvider;
import com.yokior.common.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AiChatServiceImpl implements AiChatService
{

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    // 可以注入不同的AI提供者服务实现
    @Autowired
    private AiProvider aiProvider;

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

    // 处理用户提问并返回回复
    @Override
    public ChatResponse processChat(ChatRequest request, Long userId)
    {
        // 检查会话是否存在，不存在则创建
        String sessionId = request.getSessionId();
        if (StringUtils.isEmpty(sessionId))
        {
            sessionId = this.createSession(userId);
        }
        else
        {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null || !userId.equals(session.getUserId()))
            {
                throw new ServiceException("会话不存在或无权访问");
            }
            // 更新会话时间
            session.setUpdateTime(new Date());
            chatSessionMapper.updateById(session);
        }

        // 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setId(UUID.randomUUID().toString());
        userMessage.setSessionId(sessionId);
        userMessage.setContent(request.getPrompt());
        userMessage.setRole("user");
        userMessage.setCreateTime(new Date());
        chatMessageMapper.insert(userMessage);

        // 获取聊天历史上下文(可选)
        List<ChatMessage> history = chatMessageMapper.selectRecentBySessionId(
                sessionId, 10); // 获取最近10条消息作为上下文

        // 调用AI服务获取回复
        String aiResponse = aiProvider.getCompletion(
                request.getPrompt(),
                history,
                request.getOptions()
        );

        // 保存AI回复
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setId(UUID.randomUUID().toString());
        aiMessage.setSessionId(sessionId);
        aiMessage.setContent(aiResponse);
        aiMessage.setRole("system");
        aiMessage.setCreateTime(new Date());
        chatMessageMapper.insert(aiMessage);

        // 构建响应
        ChatResponse response = new ChatResponse();
        response.setContent(aiResponse);
        response.setSessionId(sessionId);

        return response;
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
}