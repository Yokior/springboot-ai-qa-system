package com.yokior.ai.service;

import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;

import java.util.List;

/**
 * AI聊天服务接口
 */
public interface AiChatService
{
    /**
     * 创建聊天会话
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    String createSession(Long userId);
    
    /**
     * 处理聊天请求
     *
     * @param request 聊天请求对象
     * @param userId  用户ID
     * @return 聊天响应对象
     */
    ChatResponse processChat(ChatRequest request, Long userId);
    
    /**
     * 获取聊天历史记录
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 聊天消息列表
     */
    List<ChatMessage> getChatHistory(String sessionId, Long userId);
    
    /**
     * 删除聊天会话
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    void deleteSession(String sessionId, Long userId);
} 