package com.yokior.ai.controller;

import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;
import com.yokior.ai.domain.vo.ChatSessionVO;
import com.yokior.ai.service.AiChatService;
import com.yokior.common.core.controller.BaseController;
import com.yokior.common.core.domain.R;
import com.yokior.common.core.domain.model.LoginUser;
import com.yokior.common.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AiChatController extends BaseController
{

    @Autowired
    private AiChatService aiChatService;

    /**
     * 发送流式聊天消息
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamChat(@RequestBody ChatRequest request, HttpServletResponse response)
    {
        // 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
            // 委托给Service层处理
            aiChatService.processStreamChat(request, loginUser.getUserId(), response);
        }
        catch (Exception e)
        {
            log.error("AI流式聊天处理异常", e);
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    public R<Map<String, String>> createSession()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
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
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
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
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
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
     * 根据用户id获取session列表
     */
    @GetMapping("/sessions")
    public R<List<ChatSessionVO>> getSessionList()
    {
        Long userId = SecurityUtils.getUserId();
        List<ChatSessionVO> sessionList = aiChatService.getSessionVoListByUserId(userId);
        return R.ok(sessionList);
    }

    /**
     * 清空会话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public R<Void> clearHistory(@PathVariable("sessionId") String sessionId)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();

        try
        {
            aiChatService.clearChatHistory(sessionId, loginUser.getUserId());
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("清空会话历史异常", e);
            return R.fail("清空会话历史失败: " + e.getMessage());
        }
    }
}
