package com.yokior.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 上传文档元数据表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QaDocument
{

    /**
     * 文档ID，主键
     */
    private Long docId;

    /**
     * 所属团队ID (逻辑关联)
     */
    private Long teamId;

    /**
     * 文件唯一ID
     */
    private String fileId;

    /**
     * 上传者用户ID (逻辑关联)
     */
    private Long uploaderUserId;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 文件类型 (例如: pdf, docx, txt)
     */
    private String fileType;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * 文档处理状态 (PENDING, PROCESSING_TEXT, PROCESSING_NLP, COMPLETED, FAILED)
     */
    private String processingStatus;

    /**
     * 文件上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date uploadTime;

    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    /**
     * 记录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

} 