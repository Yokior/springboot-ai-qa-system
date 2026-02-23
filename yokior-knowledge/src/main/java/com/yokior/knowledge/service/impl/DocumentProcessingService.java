package com.yokior.knowledge.service.impl;

import com.yokior.common.constant.DocumentConstants;
import com.yokior.knowledge.domain.QaDocument;
import com.yokior.knowledge.domain.QaDocumentParagraph;
import com.yokior.knowledge.service.IQaDocumentService;
import com.yokior.knowledge.service.QaCacheService;
import com.yokior.knowledge.util.DocumentParser;
import com.yokior.knowledge.util.HanLPProcessor;
import com.yokior.knowledge.util.MatcherFactory;
import com.yokior.knowledge.util.ParagraphSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 文档处理服务
 */
@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    @Autowired
    private IQaDocumentService documentService;

    @Autowired
    private MatcherFactory matcherFactory;

    @Autowired
    private QaCacheService qaCacheService;

    @Value("${document.uploadPath}")
    private String uploadPathConfig;

    @Value("${document.maxParagraphLength:500}")
    private int maxParagraphLength;

    @Autowired
    private Executor documentProcessingExecutor;

    /**
     * 处理文档
     *
     * @param docId 文档ID
     */
    public void processDocument(Long docId) {
        logger.info("开始处理文档，ID: {}", docId);
        
        try {
            // 1. 获取文档信息
            QaDocument document = documentService.getDocumentById(docId);
            if (document == null) {
                logger.error("文档不存在，ID: {}", docId);
                return;
            }
            
            // 2. 更新文档状态为正在处理文本
            documentService.updateDocumentStatus(docId, DocumentConstants.PROCESSING_STATUS_PROCESSING_TEXT);
            
            // 3. 解析文档内容
            String uploadDir = System.getProperty("user.dir") + uploadPathConfig;
            String filePath = uploadDir + document.getFileId();
            String content = DocumentParser.parse(new File(filePath));
            
            // 4. 分段
            List<String> paragraphs = ParagraphSplitter.splitByNaturalParagraphs(content, maxParagraphLength);
            
            // 5. 更新文档状态为正在处理NLP
            documentService.updateDocumentStatus(docId, DocumentConstants.PROCESSING_STATUS_PROCESSING_NLP);
            
            // 6. 计算TF-IDF权重
            List<Map<String, Double>> weightsList = matcherFactory.calculateWeights(paragraphs);
            
            // 7. 创建段落对象
            List<QaDocumentParagraph> paragraphEntities = new ArrayList<>();
            for (int i = 0; i < paragraphs.size(); i++) {
                String para = paragraphs.get(i);
                
                // 分词
                List<String> terms = HanLPProcessor.segment(para);
                
                QaDocumentParagraph paragraphEntity = new QaDocumentParagraph();
                paragraphEntity.setDocId(docId);
                paragraphEntity.setTeamId(document.getTeamId());
                paragraphEntity.setContent(para);
                paragraphEntity.setParagraphOrder(i);
                paragraphEntity.setTerms(HanLPProcessor.termsToJsonString(terms));
                paragraphEntity.setFeatureWeights(matcherFactory.weightsToJsonString(weightsList.get(i)));
                paragraphEntity.setCreatedAt(new Date());
                paragraphEntity.setUpdatedAt(new Date());
                
                paragraphEntities.add(paragraphEntity);
            }
            
            // 8. 保存段落
            documentService.addDocumentParagraphs(paragraphEntities);

            // 9. 更新文档状态为已完成
            documentService.updateDocumentStatus(docId, DocumentConstants.PROCESSING_STATUS_COMPLETED);

            // 10. 清除团队问答缓存
            qaCacheService.clearTeamQaCache(document.getTeamId());

            logger.info("文档处理完成，ID: {}", docId);
        } catch (Exception e) {
            logger.error("文档处理失败，ID: {}", docId, e);
            documentService.updateDocumentStatus(docId, DocumentConstants.PROCESSING_STATUS_FAILED);
        }
    }


    public void processDocumentAsync(Long docId) {
        CompletableFuture.runAsync(() -> processDocument(docId), documentProcessingExecutor);
    }
} 