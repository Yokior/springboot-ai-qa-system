package com.yokior.ai.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 聊天响应DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse
{
    private String content;     // AI回复内容
    private String sessionId;   // 会话ID
}