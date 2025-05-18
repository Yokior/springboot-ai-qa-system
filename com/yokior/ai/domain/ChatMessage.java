package com.yokior.ai.domain;

import java.util.Date;

/**
 * AI聊天消息实体
 */
public class ChatMessage
{
    /** 消息ID */
    private String id;
    
    /** 会话ID */
    private String sessionId;
    
    /** 消息内容 */
    private String content;
    
    /** 消息类型：user(用户消息)/ai(AI回复) */
    private String type;
    
    /** 消息token数量 */
    private Integer tokenCount;
    
    /** 创建时间 */
    private Date createTime;
    
    public String getId()
    {
        return id;
    }
    
    public void setId(String id)
    {
        this.id = id;
    }
    
    public String getSessionId()
    {
        return sessionId;
    }
    
    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }
    
    public String getContent()
    {
        return content;
    }
    
    public void setContent(String content)
    {
        this.content = content;
    }
    
    public String getType()
    {
        return type;
    }
    
    public void setType(String type)
    {
        this.type = type;
    }
    
    public Integer getTokenCount()
    {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount)
    {
        this.tokenCount = tokenCount;
    }
    
    public Date getCreateTime()
    {
        return createTime;
    }
    
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
    
    @Override
    public String toString()
    {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", content='" + content + '\'' +
                ", type='" + type + '\'' +
                ", tokenCount=" + tokenCount +
                ", createTime=" + createTime +
                '}';
    }
} 