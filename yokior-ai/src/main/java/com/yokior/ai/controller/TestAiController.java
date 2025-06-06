package com.yokior.ai.controller;

import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.domain.dto.ChatRequest;
import com.yokior.ai.domain.dto.ChatResponse;
import com.yokior.ai.service.AiProvider;
import com.yokior.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2025/5/19 17:17
 */
//@RestController
//@RequestMapping("/test/ai")
@Slf4j
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


    /**
     * 发送流式聊天消息（测试用）
     * 仅请求AI并流式返回结果，不保存任何会话或消息
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamChat(@RequestBody ChatRequest request, HttpServletResponse response) throws IOException
    {
        // 设置响应头
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        // 消息内容收集器
        final StringBuilder aiResponseContent = new StringBuilder();

        try {
            log.info("收到测试流式聊天请求: {}", request.getPrompt());

            log.info("已发送开始信息");
            
            // 简单起见，不使用历史记录，直接调用AI流式接口
            List<ChatMessage> emptyHistory = new ArrayList<>();
            
            // 创建同时记录和收集内容的输出流
            final ContentCollectorOutputStream collectorStream = 
                    new ContentCollectorOutputStream(response.getOutputStream());
            
            // 直接调用AI流式API
            log.info("开始调用AI流式API");
            aiProvider.streamCompletion(
                    request.getPrompt(),
                    emptyHistory,
                    request.getOptions(),
                    collectorStream
            );
            
            // 发送结束标记
            String endMessage = "data: [DONE]\n\n";
            response.getOutputStream().write(endMessage.getBytes());
            response.getOutputStream().flush();
            
            // 记录收集到的内容
            String collectedContent = collectorStream.getCollectedContent();
            log.info("收集到的AI回复内容 ({}字符): {}", 
                    collectedContent.length(),
                    collectedContent.length() > 100 ? 
                        collectedContent.substring(0, 100) + "..." : 
                        collectedContent);
            
            log.info("测试流式聊天请求处理完成");
        } catch (Exception e) {
            log.error("测试流式聊天处理异常", e);
            try {
                // 使用SSE格式发送错误
                String errorMessage = "data: {\"error\": \"" + e.getMessage() + "\"}\n\n";
                response.getOutputStream().write(errorMessage.getBytes());
                response.getOutputStream().flush();
            } catch (Exception ex) {
                // 忽略写入错误
                log.error("写入错误响应失败", ex);
            }
        }
    }
    
    /**
     * 用于收集内容的输出流包装类
     */
    private static class ContentCollectorOutputStream extends OutputStream {
        private final OutputStream original;
        private final StringBuilder content = new StringBuilder();
        private final StringBuilder lineBuffer = new StringBuilder();
        
        public ContentCollectorOutputStream(OutputStream original) {
            this.original = original;
        }
        
        @Override
        public void write(int b) throws IOException {
            original.write(b);
            
            char c = (char) b;
            lineBuffer.append(c);
            
            if (c == '\n') {
                processLine(lineBuffer.toString());
                lineBuffer.setLength(0);
            }
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            original.write(b, off, len);
            
            String text = new String(b, off, len);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                lineBuffer.append(c);
                
                if (c == '\n') {
                    processLine(lineBuffer.toString());
                    lineBuffer.setLength(0);
                }
            }
        }
        
        private void processLine(String line) {
            line = line.trim();
            
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if (!data.equals("[DONE]")) {
                    content.append(data);
                }
            }
        }
        
        @Override
        public void flush() throws IOException {
            original.flush();
        }
        
        @Override
        public void close() throws IOException {
            if (lineBuffer.length() > 0) {
                processLine(lineBuffer.toString());
            }
            original.close();
        }
        
        public String getCollectedContent() {
            return content.toString();
        }
    }

    /**
     * 简单的SSE格式测试
     */
    @GetMapping(value = "/sse-test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void sseTest(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        
        OutputStream out = response.getOutputStream();
        
        try {
            log.info("开始SSE测试");
            
            // 先发送一个注释和初始事件，确保连接正常
            out.write("event: ping\ndata: 连接已建立\n\n".getBytes());
            out.flush();
            Thread.sleep(500);
            
            // 发送5条测试消息
            for (int i = 1; i <= 5; i++) {
                String message = "data: 这是测试消息 #" + i + "\n\n";
                out.write(message.getBytes());
                out.flush();
                log.info("发送测试消息 #{}: {}", i, message.trim());
                
                // 每条消息之间等待1秒
                Thread.sleep(1000);
            }
            
            // 发送一个特殊类型的事件
            out.write("event: custom\ndata: {\"type\": \"info\", \"message\": \"这是特殊事件\"}\n\n".getBytes());
            out.flush();
            log.info("发送了一个特殊事件");
            Thread.sleep(500);
            
            // 发送结束标记
            out.write("data: [DONE]\n\n".getBytes());
            out.flush();
            
            log.info("SSE测试完成");
        } catch (Exception e) {
            log.error("SSE测试异常", e);
            out.write(("data: {\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}\n\n").getBytes());
            out.flush();
        }
    }

    /**
     * 最简单的流式测试 - 发送纯文本，不使用SSE格式
     */
    @GetMapping(value = "/plain-stream")
    public void plainStream(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        
        OutputStream out = response.getOutputStream();
        
        try {
            log.info("开始纯文本流式测试");
            
            // 测试消息内容
            String[] messages = {
                "这是", "一个", "流式", "响应", "测试", "。",
                "每个", "部分", "会", "单独", "发送", "。"
            };
            
            // 逐个发送消息
            for (String msg : messages) {
                out.write(msg.getBytes());
                out.flush();
                log.info("发送: {}", msg);
                
                // 暂停200毫秒
                Thread.sleep(200);
            }
            
            log.info("纯文本流式测试完成");
        } catch (Exception e) {
            log.error("纯文本流式测试异常", e);
            out.write(("错误: " + e.getMessage()).getBytes());
            out.flush();
        }
    }
}
