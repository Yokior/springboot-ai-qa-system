package com.yokior.ai.controller;

import com.yokior.ai.domain.dto.ChatResponse;
import com.yokior.ai.service.AiProvider;
import com.yokior.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author Yokior
 * @description
 * @date 2025/5/19 17:17
 */
@RestController
@RequestMapping("/test/ai")
public class TestAiController
{
    @Resource(name = "deepseek")
    private AiProvider aiProvider;


    @GetMapping("/test")
    public R<ChatResponse> testChat()
    {
        String res = aiProvider.getCompletion("你好", null, null);
        return R.ok(new ChatResponse(res,"session_id"));
    }
}
