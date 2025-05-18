package com.yokior.ai.domain;

import lombok.Data;

import java.util.Date;

@Data
public class ChatSession
{
    private String id;          // 会话ID
    private Long userId;        // 用户ID
    /** 会话标题 */
    private String title;
    /** 状态（0正常 1删除） */
    private String status;
    private Date createTime;    // 创建时间
    private Date updateTime;    // 更新时间
}