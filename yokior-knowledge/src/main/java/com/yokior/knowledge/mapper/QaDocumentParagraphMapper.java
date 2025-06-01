package com.yokior.knowledge.mapper;

import com.yokior.knowledge.domain.QaDocumentParagraph;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档段落数据访问层
 */
public interface QaDocumentParagraphMapper {
    
    /**
     * 查询文档段落列表
     * 
     * @param docId 文档ID
     * @return 段落列表
     */
    List<QaDocumentParagraph> selectParagraphsByDocId(Long docId);
    
    /**
     * 查询团队所有文档段落
     * 
     * @param teamId 团队ID
     * @return 段落列表
     */
    List<QaDocumentParagraph> selectParagraphsByTeamId(Long teamId);
    
    /**
     * 添加文档段落
     * 
     * @param paragraph 段落信息
     * @return 影响行数
     */
    int insertParagraph(QaDocumentParagraph paragraph);
    
    /**
     * 批量添加文档段落
     * 
     * @param paragraphs 段落列表
     * @return 影响行数
     */
    int batchInsertParagraphs(List<QaDocumentParagraph> paragraphs);
    
    /**
     * 更新文档段落
     * 
     * @param paragraph 段落信息
     * @return 影响行数
     */
    int updateParagraph(QaDocumentParagraph paragraph);
    
    /**
     * 根据文档ID删除所有段落
     * 
     * @param docId 文档ID
     * @return 影响行数
     */
    int deleteParagraphsByDocId(Long docId);
} 