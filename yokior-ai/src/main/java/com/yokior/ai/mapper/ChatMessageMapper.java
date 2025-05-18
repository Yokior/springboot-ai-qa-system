package com.yokior.ai.mapper;

import com.yokior.ai.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI聊天消息数据层
 */
@Mapper
public interface ChatMessageMapper
{
    /**
     * 新增消息
     *
     * @param chatMessage 消息对象
     * @return 影响行数
     */
    int insert(ChatMessage chatMessage);
    
    /**
     * 根据ID查询消息
     *
     * @param id 消息ID
     * @return 消息对象
     */
    ChatMessage selectById(String id);
    
    /**
     * 根据会话ID查询所有消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID查询最近的消息
     *
     * @param sessionId 会话ID
     * @param count     消息数量
     * @return 消息列表
     */
    List<ChatMessage> selectRecentBySessionId(@Param("sessionId") String sessionId, @Param("count") int count);
    
    /**
     * 根据会话ID删除所有消息
     *
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 根据ID删除消息
     *
     * @param id 消息ID
     * @return 影响行数
     */
    int deleteById(String id);
    
    /**
     * 根据会话ID统计消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    int countBySessionId(@Param("sessionId") String sessionId);
}