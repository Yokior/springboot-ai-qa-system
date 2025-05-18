package com.yokior.ai.domain.dto;

import lombok.Data;

// 聊天响应DTO
@Data
public class ChatResponse
{
    private String content;     // AI回复内容
    private String sessionId;   // 会话ID
}