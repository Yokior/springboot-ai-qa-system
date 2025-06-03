package com.yokior.knowledge.service.impl;

import com.yokior.common.constant.DocumentConstants;
import com.yokior.knowledge.domain.QaDocument;
import com.yokior.knowledge.domain.QaDocumentParagraph;
import com.yokior.knowledge.mapper.QaDocumentMapper;
import com.yokior.knowledge.mapper.QaDocumentParagraphMapper;
import com.yokior.knowledge.util.DocumentParser;
import com.yokior.knowledge.util.HanLPProcessor;
import com.yokior.knowledge.util.ParagraphSplitter;
import com.yokior.knowledge.util.TfIdfMatcher;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文档处理服务类
 */
@Service
public class DocumentProcessingService
{

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    // 批量处理大小
    private static final int BATCH_SIZE = 100;

    // 段落最大长度
    private static final int MAX_PARAGRAPH_LENGTH = 200;

    @Autowired
    private QaDocumentMapper documentMapper;

    @Autowired
    private QaDocumentParagraphMapper paragraphMapper;

    @Value("${document.uploadPath}")
    private String uploadPathConfig;

    /**
     * 异步处理文档内容
     *
     * @param docId 文档ID
     */
    @Async("documentProcessingExecutor")
    public void processDocument(Long docId)
    {
        logger.info("开始处理文档，ID: {}", docId);

        try
        {
            // 获取文档信息
            QaDocument document = documentMapper.selectDocumentById(docId);
            if (document == null)
            {
                logger.error("文档不存在，ID: {}", docId);
                return;
            }

            // 更新文档状态为文本处理中
            updateDocumentStatus(document, DocumentConstants.PROCESSING_STATUS_PROCESSING_TEXT);

            // 获取文件路径
            String uploadDir = System.getProperty("user.dir") + uploadPathConfig;
            String filePath = uploadDir + document.getFileId();
            File file = new File(filePath);

            if (!file.exists())
            {
                logger.error("文件不存在，路径: {}", filePath);
                updateDocumentStatus(document, DocumentConstants.PROCESSING_STATUS_FAILED);
                return;
            }

            // 步骤1：解析文档
            String text = DocumentParser.parse(file);
            logger.info("文档解析完成，ID: {}, 文本长度: {}", docId, text.length());

            // 步骤2：切分段落
            List<String> paragraphs = ParagraphSplitter.splitByNaturalParagraphs(text, MAX_PARAGRAPH_LENGTH);
            logger.info("段落切分完成，ID: {}, 段落数: {}", docId, paragraphs.size());

            // 更新文档状态为NLP处理中
            updateDocumentStatus(document, DocumentConstants.PROCESSING_STATUS_PROCESSING_NLP);

            // 先删除已有的段落
            paragraphMapper.deleteParagraphsByDocId(docId);

            // 步骤3：分批处理段落
            processParagraphsInBatches(docId, document.getTeamId(), paragraphs);

            // 更新文档状态为处理完成
            updateDocumentStatus(document, DocumentConstants.PROCESSING_STATUS_COMPLETED);
            logger.info("文档处理完成，ID: {}", docId);

        }
        catch (IOException | TikaException e)
        {
            logger.error("文档处理失败，ID: " + docId, e);
            // 更新文档状态为处理失败
            QaDocument document = documentMapper.selectDocumentById(docId);
            if (document != null)
            {
                updateDocumentStatus(document, DocumentConstants.PROCESSING_STATUS_FAILED);
            }
        }
    }

    /**
     * 分批处理段落
     *
     * @param docId      文档ID
     * @param teamId     团队ID
     * @param paragraphs 段落列表
     */
    private void processParagraphsInBatches(Long docId, Long teamId, List<String> paragraphs)
    {
        int totalParagraphs = paragraphs.size();
        int batchCount = (int) Math.ceil((double) totalParagraphs / BATCH_SIZE);

        logger.info("开始分批处理段落，文档ID: {}, 总段落数: {}, 批次数: {}", docId, totalParagraphs, batchCount);

        // 先计算所有段落的TF-IDF权重
        List<Map<String, Double>> tfIdfWeightsList = TfIdfMatcher.calculateTfIdfWeights(paragraphs);

        // 分批处理
        for (int i = 0; i < batchCount; i++)
        {
            int startIndex = i * BATCH_SIZE;
            int endIndex = Math.min((i + 1) * BATCH_SIZE, totalParagraphs);

            List<QaDocumentParagraph> batchEntities = new ArrayList<>();

            for (int j = startIndex; j < endIndex; j++)
            {
                String paragraphContent = paragraphs.get(j);

                // 分词
                List<String> terms = HanLPProcessor.segment(paragraphContent);
                String termsJson = HanLPProcessor.termsToJsonString(terms);

                // TF-IDF权重
                Map<String, Double> tfIdfWeights = tfIdfWeightsList.get(j);
                String weightsJson = TfIdfMatcher.weightsToJsonString(tfIdfWeights);

                // 创建段落实体
                QaDocumentParagraph paragraph = new QaDocumentParagraph();
                paragraph.setDocId(docId);
                paragraph.setTeamId(teamId);
                paragraph.setContent(paragraphContent);
                paragraph.setParagraphOrder(j + 1); // 从1开始
                paragraph.setTerms(termsJson);
                paragraph.setFeatureWeights(weightsJson);
                paragraph.setCreatedAt(new Date());
                paragraph.setUpdatedAt(new Date());

                batchEntities.add(paragraph);
            }

            // 批量保存当前批次的段落
            if (!batchEntities.isEmpty())
            {
                batchInsertParagraphs(batchEntities);
                logger.info("批次 {}/{} 处理完成，处理了 {} 个段落", (i + 1), batchCount, batchEntities.size());
            }
        }
    }

    /**
     * 更新文档状态
     *
     * @param document 文档对象
     * @param status   状态
     */
    private void updateDocumentStatus(QaDocument document, String status)
    {
        document.setProcessingStatus(status);
        document.setUpdatedAt(new Date());
        documentMapper.updateDocument(document);
    }

    /**
     * 批量插入段落
     *
     * @param paragraphs 段落列表
     */
    @Transactional
    public void batchInsertParagraphs(List<QaDocumentParagraph> paragraphs)
    {
        if (paragraphs == null || paragraphs.isEmpty())
        {
            return;
        }

        // 批量插入段落
        paragraphMapper.batchInsertParagraphs(paragraphs);
    }

    /**
     * 保存段落信息
     *
     * @param paragraphs 段落列表
     * @deprecated 使用分批处理方法替代
     */
    @Transactional
    @Deprecated
    public void saveParagraphs(List<QaDocumentParagraph> paragraphs)
    {
        if (paragraphs == null || paragraphs.isEmpty())
        {
            return;
        }

        // 先删除已有的段落
        Long docId = paragraphs.get(0).getDocId();
        paragraphMapper.deleteParagraphsByDocId(docId);

        // 批量插入新段落
        paragraphMapper.batchInsertParagraphs(paragraphs);
    }
} 