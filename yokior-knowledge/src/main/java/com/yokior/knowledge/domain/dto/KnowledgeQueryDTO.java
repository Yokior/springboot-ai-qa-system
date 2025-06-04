package com.yokior.knowledge.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库查询参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeQueryDTO
{
    /**
     * 团队ID
     */
    private Long teamId;

    /**
     * 查询文本
     */
    private String queryText;
} 