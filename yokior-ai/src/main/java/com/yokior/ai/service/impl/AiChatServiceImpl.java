package com.yokior.ai.service.impl;

import cn.hutool.core.lang.UUID;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AiChatServiceImpl implements AiChatService
{

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;


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

    /**
     * 根据用户id获取用户会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    @Override
    public List<ChatSessionVO> getSessionVoListByUserId(Long userId)
    {
        List<ChatSessionVO> sessionVoList = chatSessionMapper.getSessionVoListByUserId(userId);

        return sessionVoList;
    }

    /**
     * 设置会话标题
     *
     * @param sessionId 会话ID
     * @param title     标题
     * @return 影响行数
     */
    @Override
    public Boolean setSessionTitle(String sessionId, String title)
    {
        int update = chatSessionMapper.updateTitle(sessionId, title);

        return update > 0;
    }

    /**
     * 清空会话历史
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 影响行数
     */
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
}