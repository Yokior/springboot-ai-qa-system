package com.yokior.ai.controller;

import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;
import com.yokior.ai.service.AiChatService;
import com.yokior.ai.service.AiProvider;
import com.yokior.common.core.controller.BaseController;
import com.yokior.common.core.domain.R;
import com.yokior.common.core.domain.model.LoginUser;
import com.yokior.common.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AiChatController extends BaseController
{

    @Autowired
    private AiChatService aiChatService;

    @Resource(name = "deepseek")
    private AiProvider aiProvider;

    /**
     * 发送聊天消息
     */
    @PostMapping("/chat")
    public R<ChatResponse> chat(@RequestBody ChatRequest request)
    {
        // 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
            // 调用AI服务处理请求
            ChatResponse response = aiChatService.processChat(request, loginUser.getUserId());
            return R.ok(response);
        }
        catch (Exception e)
        {
            log.error("AI聊天处理异常", e);
            return R.fail("AI处理失败: " + e.getMessage());
        }
    }

    /**
     * 发送流式聊天消息
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamChat(@RequestBody ChatRequest request, HttpServletResponse response) throws IOException
    {
        // 设置响应头
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        // 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();

        // 消息内容收集器
        final StringBuilder aiResponseContent = new StringBuilder();

        try
        {
            // 获取或创建会话ID
            String sessionId = request.getSessionId();
            if (StringUtils.isEmpty(sessionId))
            {
                sessionId = aiChatService.createSession(loginUser.getUserId());
            }

            final String finalSessionId = sessionId;

            // 保存用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setId(UUID.randomUUID().toString());
            userMessage.setSessionId(finalSessionId);
            userMessage.setContent(request.getPrompt());
            userMessage.setRole("user");
            userMessage.setCreateTime(new Date());
            aiChatService.saveChatMessage(userMessage);

            // 获取聊天历史
            List<ChatMessage> history = aiChatService.getChatHistory(finalSessionId, loginUser.getUserId(), 10);

            // 包装响应输出流，以便同时收集内容
            final CopyOutputStream copyOutputStream = new CopyOutputStream(response.getOutputStream(), aiResponseContent);

//            TODO: 添加用户options处理 选择不同的模型

            // 调用AI流式API
            aiProvider.streamCompletion(
                    request.getPrompt(),
                    history,
                    request.getOptions(),
                    copyOutputStream
            );

            // 处理完成后，保存AI回复
            if (aiResponseContent.length() > 0)
            {
                // 处理收集到的内容，确保格式正确
                String finalContent = aiResponseContent.toString();

                // 创建AI回复的消息记录
                ChatMessage aiMessage = new ChatMessage();
                aiMessage.setId(UUID.randomUUID().toString());
                aiMessage.setSessionId(finalSessionId);
                aiMessage.setContent(finalContent);
                aiMessage.setRole("system");
                aiMessage.setCreateTime(new Date());
                aiChatService.saveChatMessage(aiMessage);

                log.debug("保存AI回复到数据库, 长度: {} 字符", finalContent.length());
            }
        }
        catch (Exception e)
        {
            log.error("AI流式聊天处理异常", e);
            try
            {
                response.getOutputStream().write(("错误: " + e.getMessage()).getBytes());
                response.getOutputStream().flush();
            }
            catch (Exception ex)
            {
                // 忽略写入错误
            }
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    public R<Map<String, String>> createSession()
    {
        // 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
            // 创建新会话
            String sessionId = aiChatService.createSession(loginUser.getUserId());
            Map<String, String> result = new HashMap<>();
            result.put("sessionId", sessionId);
            return R.ok(result);
        }
        catch (Exception e)
        {
            log.error("创建会话异常", e);
            return R.fail("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public R<Map<String, List<ChatMessage>>> getChatHistory(@PathVariable("sessionId") String sessionId)
    {
        // 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
            // 获取聊天历史
            List<ChatMessage> messages = aiChatService.getChatHistory(sessionId, loginUser.getUserId());
            Map<String, List<ChatMessage>> result = new HashMap<>();
            result.put("messages", messages);
            return R.ok(result);
        }
        catch (Exception e)
        {
            log.error("获取聊天历史异常", e);
            return R.fail("获取聊天历史失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public R<Void> deleteSession(@PathVariable("sessionId") String sessionId)
    {
        // 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
            // 删除会话
            aiChatService.deleteSession(sessionId, loginUser.getUserId());
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("删除会话异常", e);
            return R.fail("删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 同时写入输出流和收集内容的输出流
     */
    private static class CopyOutputStream extends java.io.OutputStream
    {
        private final java.io.OutputStream target;
        private final StringBuilder collector;
        private StringBuilder lineBuffer = new StringBuilder();  // 用于临时缓存当前行

        public CopyOutputStream(java.io.OutputStream target, StringBuilder collector)
        {
            this.target = target;
            this.collector = collector;
        }

        @Override
        public void write(int b) throws IOException
        {
            target.write(b);
            char c = (char) b;

            // 处理每个字符
            lineBuffer.append(c);

            // 如果遇到换行符，处理完整行
            if (c == '\n')
            {
                processLine(lineBuffer.toString());
                lineBuffer = new StringBuilder();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            target.write(b, off, len);
            String content = new String(b, off, len);

            // 处理整块内容，可能包含多行
            for (int i = 0; i < content.length(); i++)
            {
                char c = content.charAt(i);
                lineBuffer.append(c);

                if (c == '\n')
                {
                    processLine(lineBuffer.toString());
                    lineBuffer = new StringBuilder();
                }
            }
        }

        // 处理SSE格式的行，提取实际内容
        private void processLine(String line)
        {
            // 去除末尾的换行符
            line = line.trim();

            // 只处理data行
            if (line.startsWith("data:"))
            {
                // 提取data后面的内容（去除前缀"data: "）
                String content = line.substring(5).trim();

                // 如果不是[DONE]标记，则添加到收集器
                if (!content.equals("[DONE]"))
                {
                    collector.append(content);
                }
            }
        }

        @Override
        public void flush() throws IOException
        {
            target.flush();
        }

        @Override
        public void close() throws IOException
        {
            // 处理最后可能没有换行符的数据
            if (lineBuffer.length() > 0)
            {
                processLine(lineBuffer.toString());
            }
            target.close();
        }
    }
}