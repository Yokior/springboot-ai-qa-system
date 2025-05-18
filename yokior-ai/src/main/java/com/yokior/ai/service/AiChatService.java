package com.yokior.ai.service;

import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;

import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2025/5/18 23:20
 */
public interface AiChatService
{
    /**
     * 创建会话
     * @return 会话ID
     */
    String createSession(Long userId);

    /**
     * 处理聊天请求
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse processChat(ChatRequest request, Long userId);

    /**
     * 获取聊天历史记录
     * @param sessionId 会话ID
     * @return 聊天历史记录
     */
    List<ChatMessage> getChatHistory(String sessionId, Long userId);

    /**
     * 删除会话
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId, Long userId);
}
