package com.yokior.knowledge.service.impl;

import com.yokior.common.constant.DocumentConstants;
import com.yokior.knowledge.domain.QaDocument;
import com.yokior.knowledge.domain.QaDocumentParagraph;
import com.yokior.knowledge.domain.vo.KnowledgeMatchVO;
import com.yokior.knowledge.mapper.QaDocumentMapper;
import com.yokior.knowledge.mapper.QaDocumentParagraphMapper;
import com.yokior.knowledge.service.IQaDocumentService;
import com.yokior.knowledge.util.HanLPProcessor;
import com.yokior.knowledge.util.TfIdfMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.json.JSONObject;

/**
 * 文档服务实现类
 */
@Service
public class QaDocumentServiceImpl implements IQaDocumentService
{

    @Autowired
    private QaDocumentMapper documentMapper;

    @Autowired
    private QaDocumentParagraphMapper paragraphMapper;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @Value("${document.uploadPath}")
    private String uploadPathConfig;

    // 文件上传路径
    private String uploadDir;

    @PostConstruct
    public void init()
    {
        this.uploadDir = System.getProperty("user.dir") + uploadPathConfig;
    }


    /**
     * 查询文档列表
     *
     * @param teamId           团队ID
     * @param processingStatus 处理状态
     * @return 文档列表
     */
    @Override
    public List<QaDocument> listDocuments(Long teamId, String processingStatus)
    {
        return documentMapper.selectDocumentList(teamId, processingStatus);
    }

    /**
     * 查询文档总数
     *
     * @param teamId           团队ID
     * @param processingStatus 处理状态
     * @return 总数
     */
    @Override
    public int countDocuments(Long teamId, String processingStatus)
    {
        return documentMapper.selectDocumentList(teamId, processingStatus).size();
    }

    /**
     * 根据ID查询文档
     *
     * @param docId 文档ID
     * @return 文档
     */
    @Override
    public QaDocument getDocumentById(Long docId)
    {
        return documentMapper.selectDocumentById(docId);
    }

    /**
     * 上传文档
     *
     * @param file   文件
     * @param teamId 团队ID
     * @param userId 用户ID
     * @return 文档ID
     */
    @Override
    @Transactional
    public Long uploadDocument(MultipartFile file, Long teamId, Long userId)
    {
        if (file.isEmpty())
        {
            throw new RuntimeException("文件不能为空");
        }

        // 获取文件信息
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        long fileSize = file.getSize();

        // 生成唯一文件名
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + fileType;

        // 创建文档记录
        QaDocument document = new QaDocument();
        document.setTeamId(teamId);
        document.setUploaderUserId(userId);
        document.setFileId(newFilename);
        document.setFilename(originalFilename);
        document.setFileType(fileType);
        document.setFileSize(fileSize);
        document.setProcessingStatus(DocumentConstants.PROCESSING_STATUS_PENDING); // 初始状态为待处理
        document.setUploadTime(new Date());
        document.setCreatedAt(new Date());
        document.setUpdatedAt(new Date());

        // 保存文档记录
        documentMapper.insertDocument(document);

        // 保存文件到服务器
        try
        {
            File dir = new File(uploadDir);
            if (!dir.exists())
            {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + newFilename);
            file.transferTo(dest);

            // 异步处理文档内容，解析文本和处理NLP
            documentProcessingService.processDocument(document.getDocId());

            return document.getDocId();
        }
        catch (IOException e)
        {
            // 上传失败，更新文档状态为失败
            document.setProcessingStatus(DocumentConstants.PROCESSING_STATUS_FAILED);
            documentMapper.updateDocument(document);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除文档
     *
     * @param docId 文档ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean deleteDocument(Long docId)
    {
        // 先删除文档段落
        int del1 = paragraphMapper.deleteParagraphsByDocId(docId);

        // 获取文件id也就是实际文件名
        String fileName = documentMapper.selectFileIdById(docId);

        // 再删除文档
        int del2 = documentMapper.deleteDocumentById(docId);

        if (del1 > 0 || del2 > 0)
        {
            // 删除文件 拼接文件路径
            String filePath = uploadDir + fileName;
            // 删除文件
            File file = new File(filePath);
            if (file.exists())
            {
                file.delete();
            }
        }

        return true;
    }

    /**
     * 获取文档段落列表
     *
     * @param docId 文档ID
     * @return 段落列表
     */
    @Override
    public List<QaDocumentParagraph> listDocumentParagraphs(Long docId)
    {
        return paragraphMapper.selectParagraphsByDocId(docId);
    }

    /**
     * 更新文档状态
     *
     * @param docId  文档ID
     * @param status 状态
     * @return 是否成功
     */
    @Override
    public boolean updateDocumentStatus(Long docId, String status)
    {
        QaDocument document = documentMapper.selectDocumentById(docId);
        if (document == null)
        {
            return false;
        }
        document.setProcessingStatus(status);
        document.setUpdatedAt(new Date());
        return documentMapper.updateDocument(document) > 0;
    }

    /**
     * 添加文档段落
     *
     * @param paragraphs 段落列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean addDocumentParagraphs(List<QaDocumentParagraph> paragraphs)
    {
        if (paragraphs == null || paragraphs.isEmpty())
        {
            return false;
        }

        // 批量插入段落
        return paragraphMapper.batchInsertParagraphs(paragraphs) > 0;
    }

    /**
     * 知识库检索
     *
     * @param teamId              团队ID
     * @param queryText           查询文本
     * @param maxParagraphsPerDoc 每个文档返回的最大段落数
     * @return 匹配结果列表
     */
    @Override
    public List<KnowledgeMatchVO> searchKnowledge(Long teamId, String queryText, Integer maxParagraphsPerDoc)
    {
        // 1. 检查输入参数
        if (queryText == null || queryText.trim().isEmpty())
        {
            return new ArrayList<>();
        }

        if (maxParagraphsPerDoc == null || maxParagraphsPerDoc <= 0)
        {
            maxParagraphsPerDoc = 3; // 默认每个文档返回3个段落
        }

        // 2. 获取已处理完成的团队文档列表
        List<QaDocument> documents = documentMapper.selectDocumentList(teamId, DocumentConstants.PROCESSING_STATUS_COMPLETED);
        if (documents.isEmpty())
        {
            return new ArrayList<>();
        }

        // 3. 对查询文本进行分词
        List<String> queryTerms = HanLPProcessor.segment(queryText);
        if (queryTerms.isEmpty())
        {
            return new ArrayList<>();
        }

        // 4. 创建结果集
        List<KnowledgeMatchVO> results = new ArrayList<>();

        // 5. 按文档分组处理
        for (QaDocument document : documents)
        {
            // 获取文档的所有段落
            List<QaDocumentParagraph> paragraphs = paragraphMapper.selectParagraphsByDocId(document.getDocId());
            if (paragraphs.isEmpty())
            {
                continue;
            }

            // 提取段落内容列表，用于TF-IDF匹配
            List<String> paragraphContents = paragraphs.stream()
                    .map(QaDocumentParagraph::getContent)
                    .collect(Collectors.toList());

            // 匹配查询词与段落
            List<Integer> matchedIndices = TfIdfMatcher.match(queryTerms, paragraphContents);

            // 取前N个最匹配的段落
            int count = 0;
            Map<Integer, Double> scoreMap = calculateScores(queryTerms, paragraphs);

            for (Integer idx : matchedIndices)
            {
                if (count >= maxParagraphsPerDoc)
                {
                    break;
                }

                if (idx >= 0 && idx < paragraphs.size())
                {
                    QaDocumentParagraph paragraph = paragraphs.get(idx);

                    KnowledgeMatchVO matchVO = new KnowledgeMatchVO();
                    matchVO.setDocId(document.getDocId());
                    matchVO.setFilename(document.getFilename());
                    matchVO.setParagraphId(paragraph.getParagraphId());
                    matchVO.setContent(paragraph.getContent());
                    matchVO.setParagraphOrder(paragraph.getParagraphOrder());
                    matchVO.setScore(scoreMap.getOrDefault(idx, 0.0));

                    results.add(matchVO);
                    count++;
                }
            }
        }

        // 6. 按得分降序排序结果
        results.sort(Comparator.comparing(KnowledgeMatchVO::getScore).reversed());

        return results;
    }

    /**
     * 计算查询词与段落的匹配得分
     *
     * @param queryTerms 查询词条
     * @param paragraphs 段落列表
     * @return 索引-得分映射
     */
    private Map<Integer, Double> calculateScores(List<String> queryTerms, List<QaDocumentParagraph> paragraphs)
    {
        Map<Integer, Double> scoreMap = new HashMap<>();

        for (int i = 0; i < paragraphs.size(); i++)
        {
            QaDocumentParagraph paragraph = paragraphs.get(i);
            String featureWeightsJson = paragraph.getFeatureWeights();

            // 如果权重为空，则跳过
            if (featureWeightsJson == null || featureWeightsJson.isEmpty())
            {
                scoreMap.put(i, 0.0);
                continue;
            }

            try
            {
                // 解析TF-IDF权重JSON
                JSONObject weights = new JSONObject(featureWeightsJson);
                double score = 0.0;

                // 累加每个查询词的TF-IDF得分
                for (String term : queryTerms)
                {
                    if (weights.has(term))
                    {
                        score += weights.getDouble(term);
                    }
                }

                scoreMap.put(i, score);
            }
            catch (Exception e)
            {
                scoreMap.put(i, 0.0);
            }
        }

        return scoreMap;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private String getFileExtension(String filename)
    {
        if (filename == null)
        {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0)
        {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
} 