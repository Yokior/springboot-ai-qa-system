package com.yokior.ai.domain.dto;

import lombok.Data;

import java.util.Map;

// 聊天请求DTO
@Data
public class ChatRequest
{
    private String prompt;               // 用户输入的问题
    private String sessionId;            // 会话ID
    private Map<String, Object> options; // 配置选项
}