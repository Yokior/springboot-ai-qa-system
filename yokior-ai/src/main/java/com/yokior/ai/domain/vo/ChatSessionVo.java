package com.yokior.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * @author Yokior
 * @description
 * @date 2025/6/6 16:35
 */
@Data
public class ChatSessionVo
{
    private String sessionId;

    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String createTime;
}
