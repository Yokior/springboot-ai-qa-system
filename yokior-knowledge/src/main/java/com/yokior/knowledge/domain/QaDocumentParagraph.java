package com.yokior.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 文档段落内容与特征表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QaDocumentParagraph
{

    /**
     * 段落ID
     */
    private Long paragraphId;

    /**
     * 所属文档ID (逻辑关联 qa_document.doc_id)
     */
    private Long docId;

    /**
     * 所属团队ID (逻辑关联 qa_document.team_id, 同文档表team_id，方便直接按团队查询段落)
     */
    private Long teamId;

    /**
     * 段落的纯文本内容
     */
    private String content;

    /**
     * 段落在文档中的顺序 (从0或1开始，重要)
     */
    private Integer paragraphOrder;

    /**
     * 分词结果 (例如: ["词1", "词2"])
     */
    private String terms;

    /**
     * 特征权重 (例如 TF-IDF: {"term1": 0.5, "term2": 0.3} 或其他算法的权重)
     */
    private String featureWeights;

    /**
     * 段落记录创建时间 (即处理完成时间)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    /**
     * 记录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

} 