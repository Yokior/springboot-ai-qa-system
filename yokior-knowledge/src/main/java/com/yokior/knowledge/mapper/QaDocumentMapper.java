package com.yokior.knowledge.mapper;

import com.yokior.knowledge.domain.QaDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档数据访问层
 */
public interface QaDocumentMapper {
    
    /**
     * 查询文档列表
     * 
     * @param teamId 团队ID
     * @param processingStatus 处理状态
     * @return 文档列表
     */
    List<QaDocument> selectDocumentList(@Param("teamId") Long teamId, @Param("processingStatus") String processingStatus);
    
    /**
     * 根据ID查询文档
     * 
     * @param docId 文档ID
     * @return 文档
     */
    QaDocument selectDocumentById(Long docId);

    /**
     * 根据ID查询文档文件ID
     *
     * @param docId 文档ID
     * @return 文档文件ID
     */
    String selectFileIdById(Long docId);

    
    /**
     * 添加文档
     * 
     * @param document 文档信息
     * @return 影响行数
     */
    int insertDocument(QaDocument document);
    
    /**
     * 更新文档
     * 
     * @param document 文档信息
     * @return 影响行数
     */
    int updateDocument(QaDocument document);
    
    /**
     * 删除文档
     * 
     * @param docId 文档ID
     * @return 影响行数
     */
    int deleteDocumentById(Long docId);
} 