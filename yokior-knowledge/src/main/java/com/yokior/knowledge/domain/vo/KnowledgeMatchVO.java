package com.yokior.knowledge.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库匹配结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeMatchVO {
    
    /**
     * 文档ID
     */
    private Long docId;
    
    /**
     * 文档名称
     */
    private String filename;
    
    /**
     * 段落ID
     */
    private Long paragraphId;
    
    /**
     * 段落内容
     */
    private String content;
    
    /**
     * 段落在文档中的顺序
     */
    private Integer paragraphOrder;
    
    /**
     * 匹配得分
     */
    private Double score;
    
    /**
     * 匹配算法类型（TF_IDF 或 BM25）
     */
    private String matcherType;
} 