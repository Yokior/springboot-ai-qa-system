package com.yokior.knowledge.controller;

import com.yokior.common.annotation.TeamAuth;
import com.yokior.common.constant.TeamConstants;
import com.yokior.common.core.controller.BaseController;
import com.yokior.common.core.domain.AjaxResult;
import com.yokior.common.core.page.TableDataInfo;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.knowledge.domain.QaDocument;
import com.yokior.knowledge.domain.QaDocumentParagraph;
import com.yokior.knowledge.domain.dto.DocumentDTO;
import com.yokior.knowledge.domain.dto.KnowledgeQueryDTO;
import com.yokior.knowledge.domain.vo.KnowledgeMatchVO;
import com.yokior.knowledge.service.IQaDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/knowledge/document")
public class QaDocumentController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(QaDocumentController.class);

    @Autowired
    private IQaDocumentService documentService;

    /**
     * 获取文档列表
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN, TeamConstants.ROLE_MEMBER})
    @GetMapping("/list")
    public TableDataInfo list(DocumentDTO documentDTO)
    {
        log.info("获取文档列表, 参数: {}", documentDTO);
        startPage();
        List<QaDocument> list = documentService.listDocuments(
                documentDTO.getTeamId(),
                documentDTO.getProcessingStatus());
        return getDataTable(list);
    }

    /**
     * 获取文档详情
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN, TeamConstants.ROLE_MEMBER})
    @GetMapping("/{docId}")
    public AjaxResult getInfo(@PathVariable Long docId)
    {
        log.info("获取文档详情, docId: {}", docId);
        return success(documentService.getDocumentById(docId));
    }

    /**
     * 上传文档
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN})
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file, @RequestParam("teamId") Long teamId)
    {
        log.info("上传文档, 文件名: {}, teamId: {}", file.getOriginalFilename(), teamId);
        Long userId = SecurityUtils.getUserId();
        Long docId = documentService.uploadDocument(file, teamId, userId);
        log.info("文档上传成功, docId: {}", docId);
        return success(docId);
    }

    /**
     * 删除文档
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN})
    @DeleteMapping("/{docId}")
    public AjaxResult remove(@PathVariable Long docId)
    {
        log.info("删除文档, docId: {}", docId);
        boolean result = documentService.deleteDocument(docId);
        return toAjax(result);
    }

    /**
     * 获取文档段落列表
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN, TeamConstants.ROLE_MEMBER})
    @GetMapping("/paragraphs/{docId}")
    public AjaxResult paragraphs(@PathVariable Long docId)
    {
        log.info("获取文档段落列表, docId: {}", docId);
        List<QaDocumentParagraph> paragraphs = documentService.listDocumentParagraphs(docId);
        return success(paragraphs);
    }
    
    /**
     * 知识库检索
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN, TeamConstants.ROLE_MEMBER})
    @PostMapping("/search")
    public AjaxResult search(@RequestBody KnowledgeQueryDTO queryDTO)
    {
        log.info("知识库检索, 参数: {}", queryDTO);
        List<KnowledgeMatchVO> matchResults = documentService.searchKnowledge(
                queryDTO.getTeamId(),
                queryDTO.getQueryText(), 3);
        return success(matchResults);
    }
} 