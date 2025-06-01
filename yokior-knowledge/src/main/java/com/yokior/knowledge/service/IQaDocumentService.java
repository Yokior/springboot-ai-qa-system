package com.yokior.knowledge.service;

import com.yokior.knowledge.domain.QaDocument;
import com.yokior.knowledge.domain.QaDocumentParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 */
public interface IQaDocumentService
{

    /**
     * 查询文档列表
     *
     * @param teamId           团队ID
     * @param processingStatus 处理状态
     * @param pageNum          页码
     * @param pageSize         每页大小
     * @return 文档列表
     */
    List<QaDocument> listDocuments(Long teamId, String processingStatus, Integer pageNum, Integer pageSize);

    /**
     * 查询文档总数
     *
     * @param teamId           团队ID
     * @param processingStatus 处理状态
     * @return 总数
     */
    int countDocuments(Long teamId, String processingStatus);

    /**
     * 根据ID查询文档
     *
     * @param docId 文档ID
     * @return 文档
     */
    QaDocument getDocumentById(Long docId);

    /**
     * 上传文档
     *
     * @param file   文件
     * @param teamId 团队ID
     * @param userId 用户ID
     * @return 文档ID
     */
    Long uploadDocument(MultipartFile file, Long teamId, Long userId);

    /**
     * 删除文档
     *
     * @param docId 文档ID
     * @return 是否成功
     */
    boolean deleteDocument(Long docId);

    /**
     * 获取文档段落列表
     *
     * @param docId 文档ID
     * @return 段落列表
     */
    List<QaDocumentParagraph> listDocumentParagraphs(Long docId);

    /**
     * 更新文档状态
     *
     * @param docId  文档ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateDocumentStatus(Long docId, String status);

    /**
     * 添加文档段落
     *
     * @param paragraphs 段落列表
     * @return 是否成功
     */
    boolean addDocumentParagraphs(List<QaDocumentParagraph> paragraphs);
} 