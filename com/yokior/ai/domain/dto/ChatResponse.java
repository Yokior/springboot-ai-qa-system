package com.yokior.ai.domain.dto;

/**
 * 聊天响应DTO
 */
public class ChatResponse
{
    /** AI回复内容 */
    private String content;
    
    /** 会话ID */
    private String sessionId;
    
    public String getContent()
    {
        return content;
    }
    
    public void setContent(String content)
    {
        this.content = content;
    }
    
    public String getSessionId()
    {
        return sessionId;
    }
    
    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }
} 