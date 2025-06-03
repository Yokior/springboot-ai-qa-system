package com.yokior.knowledge.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 段落切分工具类
 */
public class ParagraphSplitter
{

    /**
     * 按最大长度切分文本为段落
     *
     * @param text      文本内容
     * @param maxLength 每段最大长度
     * @return 切分后的段落列表
     */
    public static List<String> split(String text, int maxLength)
    {
        List<String> result = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += maxLength)
        {
            int end = Math.min(i + maxLength, length);
            result.add(text.substring(i, end));
        }
        return result;
    }

    /**
     * 按自然段落切分文本
     *
     * @param text      文本内容
     * @param maxLength 每段最大长度（如果自然段落超过这个长度，会被进一步切分）
     * @return 切分后的段落列表
     */
    public static List<String> splitByNaturalParagraphs(String text, int maxLength)
    {
        List<String> result = new ArrayList<>();

        // 按换行符切分
        String[] naturalParagraphs = text.split("\\r?\\n\\r?\\n");

        for (String paragraph : naturalParagraphs)
        {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty())
            {
                continue;
            }

            // 如果段落长度超过最大长度，进一步切分
            if (paragraph.length() > maxLength)
            {
                result.addAll(split(paragraph, maxLength));
            }
            else
            {
                result.add(paragraph);
            }
        }

        return result;
    }
} 