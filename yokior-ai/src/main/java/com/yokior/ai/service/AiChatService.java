package com.yokior.ai.service;

import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;
import com.yokior.ai.domain.vo.ChatSessionVO;
import com.yokior.knowledge.domain.vo.KnowledgeMatchVO;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Yokior
 * @description
 * @date 2025/5/18 23:20
 */
public interface AiChatService
{
    /**
     * 创建新聊天会话
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    String createSession(Long userId);

    /**
     * 处理聊天请求
     *
     * @param request 聊天请求
     * @param userId  用户ID
     * @return 聊天响应
     */
    ChatResponse processChat(ChatRequest request, Long userId);

    /**
     * 获取会话的聊天历史
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 消息列表
     */
    List<ChatMessage> getChatHistory(String sessionId, Long userId);

    /**
     * 获取会话的最近聊天历史（指定数量）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param count     消息数量
     * @return 消息列表
     */
    List<ChatMessage> getChatHistory(String sessionId, Long userId, int count);

    /**
     * 保存聊天消息
     *
     * @param message 聊天消息
     */
    void saveChatMessage(ChatMessage message);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     */
    void deleteSession(String sessionId, Long userId);

    /**
     * 根据用户id获取用户会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSessionVO> getSessionVoListByUserId(Long userId);

    /**
     * 设置会话标题
     *
     * @param sessionId 会话ID
     * @param title     标题
     * @return 影响行数
     */
    Boolean setSessionTitle(String sessionId, String title);

    /**
     * 清空会话聊天历史
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 影响行数
     */
    Boolean clearChatHistory(String sessionId, Long userId);

    /**
     * 处理流式聊天请求
     *
     * @param request  聊天请求
     * @param userId   用户ID
     * @param response HTTP响应对象
     * @throws Exception 处理异常
     */
    void processStreamChat(ChatRequest request, Long userId, HttpServletResponse response) throws Exception;

    /**
     * 根据用户选项获取AI提供者
     *
     * @param options 用户选项
     * @return AI提供者名称
     */
    String getAiProviderName(Map<String, Object> options);

    /**
     * 构建知识库增强的Prompt
     *
     * @param originalPrompt 原始问题
     * @param teamId         团队ID
     * @return 构建后的Prompt和知识匹配结果
     */
    KnowledgePromptResult buildKnowledgePrompt(String originalPrompt, Long teamId);

    /**
     * 知识库Prompt构建结果
     */
    class KnowledgePromptResult {
        private String prompt;
        private String questionHash;
        private List<KnowledgeMatchVO> knowledgeMatches;
        private boolean isFrequentQuestion;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getQuestionHash() { return questionHash; }
        public void setQuestionHash(String questionHash) { this.questionHash = questionHash; }
        public List<KnowledgeMatchVO> getKnowledgeMatches() { return knowledgeMatches; }
        public void setKnowledgeMatches(List<KnowledgeMatchVO> knowledgeMatches) { this.knowledgeMatches = knowledgeMatches; }
        public boolean isFrequentQuestion() { return isFrequentQuestion; }
        public void setFrequentQuestion(boolean frequentQuestion) { isFrequentQuestion = frequentQuestion; }
    }
}
