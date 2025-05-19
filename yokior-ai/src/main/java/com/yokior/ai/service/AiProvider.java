package com.yokior.ai.service;

import com.yokior.ai.domain.ChatMessage;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * AI提供者接口
 */
public interface AiProvider
{
    /**
     * 获取AI文本补全
     *
     * @param prompt  用户输入的问题
     * @param history 历史消息记录(可选)
     * @param options 配置选项(可选)
     * @return AI回复的内容
     */
    String getCompletion(String prompt, List<ChatMessage> history, Map<String, Object> options);

    /**
     * 获取AI流式文本补全
     *
     * @param prompt  用户输入的问题
     * @param history 历史消息记录(可选)
     * @param options 配置选项(可选)
     * @param output 输出流
     */
    void streamCompletion(String prompt, List<ChatMessage> history, Map<String, Object> options, OutputStream output);
}