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
     * 创建新聊天会话
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    String createSession(Long userId);

    /**
     * 处理聊天请求
     *
     * @param request 聊天请求
     * @param userId  用户ID
     * @return 聊天响应
     */
    ChatResponse processChat(ChatRequest request, Long userId);

    /**
     * 获取会话的聊天历史
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 消息列表
     */
    List<ChatMessage> getChatHistory(String sessionId, Long userId);
    
    /**
     * 获取会话的最近聊天历史（指定数量）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param count     消息数量
     * @return 消息列表
     */
    List<ChatMessage> getChatHistory(String sessionId, Long userId, int count);

    /**
     * 保存聊天消息
     *
     * @param message 聊天消息
     */
    void saveChatMessage(ChatMessage message);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    void deleteSession(String sessionId, Long userId);

    /**
     * 根据用户id获取用户会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<String> getSessionListByUserId(Long userId);
}
