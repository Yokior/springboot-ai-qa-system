package com.yokior.knowledge.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author Yokior
 * @description 文档详情VO
 * @date 2025/6/7 16:20
 */
@Data
public class DocumentDetailVO
{
    /**
     * 上传者用户名称
     */
    private String uploaderUserName;

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
