package com.yokior.knowledge.service.impl;

import com.yokior.common.constant.DocumentConstants;
import com.yokior.knowledge.domain.QaDocument;
import com.yokior.knowledge.domain.QaDocumentParagraph;
import com.yokior.knowledge.mapper.QaDocumentMapper;
import com.yokior.knowledge.mapper.QaDocumentParagraphMapper;
import com.yokior.knowledge.service.IQaDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
        // TODO: 实现文档总数查询
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

            // TODO: 异步处理文档内容，解析文本和处理NLP
            // 这里应该启动一个异步任务来处理文档内容，更新文档状态并保存段落信息

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