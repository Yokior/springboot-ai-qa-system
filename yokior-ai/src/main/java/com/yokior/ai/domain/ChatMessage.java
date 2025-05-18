package com.yokior.ai.domain;

import lombok.Data;

import java.util.Date;

@Data
public class ChatMessage
{
    private String id;           // 消息ID
    private String sessionId;    // 会话ID
    private String content;      // 消息内容
    private String type;         // 消息类型：user/ai
    /** 消息token数量 */
    private Integer tokenCount;
    private Date createTime;     // 创建时间

}