package com.yokior.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage
{
    private String id;           // 消息ID
    private String sessionId;    // 会话ID
    private String content;      // 消息内容
    private String role;         // 消息发送者：user/system
    /** 消息token数量 */
    private Integer tokenCount;
    private Date createTime;     // 创建时间

}