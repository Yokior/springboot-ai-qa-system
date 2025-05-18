package com.yokior.ai.domain.dto;

import java.util.Map;

/**
 * 聊天请求DTO
 */
public class ChatRequest
{
    /** 用户输入的问题 */
    private String prompt;
    
    /** 会话ID */
    private String sessionId;
    
    /** 配置选项 */
    private Map<String, Object> options;
    
    public String getPrompt()
    {
        return prompt;
    }
    
    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }
    
    public String getSessionId()
    {
        return sessionId;
    }
    
    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }
    
    public Map<String, Object> getOptions()
    {
        return options;
    }
    
    public void setOptions(Map<String, Object> options)
    {
        this.options = options;
    }
} 