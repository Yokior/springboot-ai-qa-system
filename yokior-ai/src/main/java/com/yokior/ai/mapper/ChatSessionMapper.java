package com.yokior.ai.mapper;

import com.yokior.ai.domain.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI聊天会话数据层
 */
@Mapper
public interface ChatSessionMapper
{
    /**
     * 新增会话
     *
     * @param chatSession 会话对象
     * @return 影响行数
     */
    int insert(ChatSession chatSession);
    
    /**
     * 根据ID查询会话
     *
     * @param id 会话ID
     * @return 会话对象
     */
    ChatSession selectById(String id);
    
    /**
     * 更新会话
     *
     * @param chatSession 会话对象
     * @return 影响行数
     */
    int updateById(ChatSession chatSession);
    
    /**
     * 删除会话
     *
     * @param id 会话ID
     * @return 影响行数
     */
    int deleteById(String id);
    
    /**
     * 查询用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据会话ID和用户ID查询会话
     *
     * @param id     会话ID
     * @param userId 用户ID
     * @return 会话对象
     */
    ChatSession selectByIdAndUserId(@Param("id") String id, @Param("userId") Long userId);
    
    /**
     * 更新会话标题
     *
     * @param id    会话ID
     * @param title 会话标题
     * @return 影响行数
     */
    int updateTitle(@Param("id") String id, @Param("title") String title);
}