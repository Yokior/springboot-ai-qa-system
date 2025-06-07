package com.yokior.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yokior.ai.domain.ChatMessage;
import com.yokior.ai.service.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yokior
 * @description
 * @date 2025/5/19 14:40
 */
@Service("DeepSeek")
public class DeepSeekAiProviderImpl implements AiProvider
{
    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiProviderImpl.class);

    @Value("${ai.providers.DeepSeek.apiKey}")
    private String apiKey;

    @Value("${ai.providers.DeepSeek.model}")
    private String model;

    @Value("${ai.providers.DeepSeek.endpoint}")
    private String endpoint;

    @Value("${ai.providers.DeepSeek.temperature}")
    private Double temperature;

    @Value("${ai.providers.DeepSeek.maxTokens:2000}")
    private Integer maxTokens;

    @Value("${ai.providers.DeepSeek.stream}")
    private Boolean stream;


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekAiProviderImpl(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取模型回答
     *
     * @param prompt
     * @param history
     * @param options
     * @return
     */
    @Override
    public String getCompletion(String prompt, List<ChatMessage> history, Map<String, Object> options)
    {
        try
        {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();

            if (history != null)
            {
                for (ChatMessage msg : history)
                {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", msg.getRole());
                    m.put("content", msg.getContent());
                    messages.add(m);
                }
            }

            // 添加当前问题
            Map<String, String> currentMsg = new HashMap<>();
            currentMsg.put("role", "user");
            currentMsg.put("content", prompt);
            messages.add(currentMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            // 设置 Header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint, entity, String.class);

            // 返回模型的回答
            return parseResponse(response.getBody());

        }
        catch (Exception e)
        {
            throw new RuntimeException("调用 DeepSeek 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式获取模型回答
     *
     * @param prompt  用户问题
     * @param history 历史消息
     * @param options 配置选项
     * @param output  输出流
     */
    @Override
    public void streamCompletion(String prompt, List<ChatMessage> history, Map<String, Object> options, OutputStream output)
    {
        try
        {
            log.info("开始处理流式请求: {}", prompt);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("stream", true);

            List<Map<String, String>> messages = new ArrayList<>();

            if (history != null)
            {
                for (ChatMessage msg : history)
                {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", msg.getRole());
                    m.put("content", msg.getContent());
                    messages.add(m);
                }
                log.debug("历史消息数量: {}", history.size());
            }

            // 添加当前问题
            Map<String, String> currentMsg = new HashMap<>();
            currentMsg.put("role", "user");
            currentMsg.put("content", prompt);
            messages.add(currentMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            // 创建HTTP连接并发送请求
            URL url = new URL(endpoint);
            log.debug("请求地址: {}", endpoint);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            // 发送请求体
            try (OutputStream out = conn.getOutputStream())
            {
                byte[] requestBytes = objectMapper.writeValueAsBytes(requestBody);
                out.write(requestBytes);
                out.flush();
                log.debug("已发送请求, 大小: {} 字节", requestBytes.length);
            }

            // 检查响应状态
            int responseCode = conn.getResponseCode();
            log.debug("API响应状态码: {}", responseCode);

            if (responseCode != 200)
            {
                String errorMsg = "API返回错误: HTTP " + responseCode;
                log.error(errorMsg);
                // 以SSE格式返回错误
                output.write(("data: {\"error\": \"" + errorMsg + "\"}\n\n").getBytes());
                output.flush();
                throw new RuntimeException(errorMsg);
            }

            // 读取响应（现在我们处理两种情况：流式响应和普通响应）
            StringBuilder fullResponse = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream())))
            {
                String line;
                log.debug("开始读取响应");
                int messageCount = 0;
                boolean isStreamResponse = false;

                while ((line = reader.readLine()) != null)
                {
                    if (line.isEmpty()) continue;

                    // 对原始响应行进行日志记录
//                    log.debug("收到响应行: {}", line);
                    fullResponse.append(line);

                    // 检查是否是流式响应的开始
                    if (line.startsWith("data:"))
                    {
                        isStreamResponse = true;

                        // 处理[DONE]特殊消息
                        if (line.equals("data: [DONE]"))
                        {
                            log.debug("收到结束标记");
                            continue;
                        }

                        // 移除 "data: " 前缀
                        String content = line.substring(6);

                        try
                        {
                            // 解析 SSE 数据
                            Map<String, Object> data = objectMapper.readValue(content, Map.class);
                            List<?> choices = (List<?>) data.get("choices");
                            if (choices != null && !choices.isEmpty())
                            {
                                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                                Map<?, ?> delta = (Map<?, ?>) choice.get("delta");

                                // 提取文本内容
                                if (delta != null && delta.containsKey("content"))
                                {
                                    String chunk = (String) delta.get("content");

                                    if (chunk == null || chunk.isEmpty())
                                    {
                                        continue;
                                    }

                                    // 以SSE格式发送内容，包装为单独的消息对象
                                    Map<String, Object> messageObject = new HashMap<>();
                                    messageObject.put("content", chunk);
                                    String formattedContent = "data: " + objectMapper.writeValueAsString(messageObject) + "\n\n";
                                    output.write(formattedContent.getBytes());
                                    output.flush();

                                    messageCount++;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            log.warn("解析SSE响应失败: {}", e.getMessage());
                            // 如果解析失败，尝试直接发送原始行
                            String safeLine = "data: " + objectMapper.writeValueAsString(content) + "\n\n";
                            output.write(safeLine.getBytes());
                            output.flush();
                        }
                    }
                }

                // 如果不是流式响应，我们需要处理完整的JSON响应
                if (!isStreamResponse && fullResponse.length() > 0)
                {
                    log.info("收到非流式响应，长度: {} 字符", fullResponse.length());
                    try
                    {
                        // 解析完整的JSON响应
                        Map<String, Object> data = objectMapper.readValue(fullResponse.toString(), Map.class);
                        List<?> choices = (List<?>) data.get("choices");
                        if (choices != null && !choices.isEmpty())
                        {
                            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                            Map<?, ?> message = (Map<?, ?>) choice.get("message");

                            if (message != null && message.containsKey("content"))
                            {
                                String content = (String) message.get("content");
                                log.info("提取到完整内容，长度: {} 字符", content.length());

                                // 模拟流式输出，将完整回复分割成小块发送
                                simulateStreamOutput(content, output);
                                messageCount = 1; // 至少发送了一条消息
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("解析完整JSON响应失败: {}", e.getMessage());
                        output.write(("data: {\"error\": \"解析响应失败: " + e.getMessage() + "\"}\n\n").getBytes());
                        output.flush();
                    }
                }

                log.info("响应处理完成，共发送 {} 条消息", messageCount);
            }
        }
        catch (Exception e)
        {
            log.error("流式处理失败: {}", e.getMessage(), e);
            try
            {
                // 以SSE格式返回错误
                String errorMsg = "data: {\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}\n\n";
                output.write(errorMsg.getBytes());
                output.flush();
            }
            catch (IOException ex)
            {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    /**
     * 模拟流式输出，将长文本分成小块输出
     */
    private void simulateStreamOutput(String content, OutputStream output) throws IOException
    {
        log.info("开始模拟流式输出");

        // 按自然段落分割（先按换行符分割）
        String[] paragraphs = content.split("\n\n");
        if (paragraphs.length == 1)
        {
            // 如果没有双换行，尝试按单换行分割
            paragraphs = content.split("\n");
        }

        if (paragraphs.length == 1)
        {
            // 如果仍然没有换行，尝试按句号分割
            paragraphs = content.split("。|\\.|！|\\!|\\?|？");
        }

        if (paragraphs.length == 1 && content.length() > 20)
        {
            // 如果仍然是一整块，且内容较长，按固定长度切分
            int chunkSize = 20; // 每块20个字符
            paragraphs = new String[(content.length() + chunkSize - 1) / chunkSize];
            for (int i = 0; i < paragraphs.length; i++)
            {
                int start = i * chunkSize;
                int end = Math.min(content.length(), (i + 1) * chunkSize);
                paragraphs[i] = content.substring(start, end);
            }
        }

        // 如果太少的块，添加一些延迟使其看起来像流式响应
        for (String paragraph : paragraphs)
        {
            if (!paragraph.trim().isEmpty())
            {
                // 创建与流式响应格式一致的消息对象
                Map<String, Object> messageObject = new HashMap<>();
                messageObject.put("content", paragraph);
                
                // 格式化为SSE格式并发送
                String formatted = "data: " + objectMapper.writeValueAsString(messageObject) + "\n\n";
                output.write(formatted.getBytes());
                output.flush();
                log.debug("模拟流输出块: {}", paragraph);

                try
                {
                    // 添加一点延迟，模拟真实的流式输出
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("模拟流式输出完成，共 {} 个块", paragraphs.length);
    }

    private String parseResponse(String responseBody) throws Exception
    {
        // 解析 JSON 响应
        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
        List<?> choices = (List<?>) map.get("choices");
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return (String) message.get("content");
    }
}
