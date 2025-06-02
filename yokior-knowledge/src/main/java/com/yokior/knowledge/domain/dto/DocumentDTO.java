package com.yokior.knowledge.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档数据传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDTO
{

    /**
     * 团队ID
     */
    private Long teamId;

    /**
     * 文档处理状态
     */
    private String processingStatus;

} 