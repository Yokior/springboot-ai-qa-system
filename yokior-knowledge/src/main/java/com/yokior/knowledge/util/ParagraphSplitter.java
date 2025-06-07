package com.yokior.knowledge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 段落切分工具类
 * 针对不同类型的文档采用不同的分段策略
 */
public class ParagraphSplitter {
    private static final Logger logger = LoggerFactory.getLogger(ParagraphSplitter.class);

    /**
     * 代码块开始标记的正则表达式
     */
    private static final Pattern CODE_BLOCK_START_PATTERN = Pattern.compile("(public|private|protected|class|interface|enum|abstract|static|void|\\{)\\s*");
    
    /**
     * 代码块结束标记的正则表达式
     */
    private static final Pattern CODE_BLOCK_END_PATTERN = Pattern.compile("(\\}|;)\\s*$");
    
    /**
     * 编号模式识别（如 1. 2. 3. 或 (1) (2) (3) 或 一、二、三、等）
     */
    private static final Pattern NUMBERED_ITEM_PATTERN = Pattern.compile("^\\s*(\\d+\\.|\\\\.\\d+|[一二三四五六七八九十]+、|\\([一二三四五六七八九十]\\)|\\([0-9a-zA-Z]+\\)|【.*】)\\s*.*");
    
    /**
     * 最小有效段落长度（字符数）
     */
    private static final int MIN_PARAGRAPH_LENGTH = 20;
    
    /**
     * 最小合并段落长度（字符数）
     */
    private static final int MIN_MERGE_LENGTH = 100;
    
    /**
     * 按自然段落切分文本
     *
     * @param text      文本内容
     * @param maxLength 每段最大长度（如果自然段落超过这个长度，会被进一步切分）
     * @return 切分后的段落列表
     */
    public static List<String> splitByNaturalParagraphs(String text, int maxLength) {
        // 预处理：规范化换行符和空格
        text = preprocessText(text);
        
        // 初步分段处理
        List<String> initialSegments = initialSegmentation(text);
        
        // 合并短段落和相关段落
        List<String> mergedSegments = mergeRelatedSegments(initialSegments, maxLength);
        
        // 处理过长段落
        List<String> finalSegments = handleLongSegments(mergedSegments, maxLength);
        
        logger.info("文本分段完成: 原始文本长度={}, 分段数量={}", text.length(), finalSegments.size());
        return finalSegments;
    }
    
    /**
     * 预处理文本：规范化换行符和空格
     */
    private static String preprocessText(String text) {
        // 统一换行符
        text = text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        
        // 移除多余空格
        text = text.replaceAll(" {2,}", " ");
        
        // 处理可能的PDF特殊格式问题
        text = text.replaceAll("\\n\\s*\\n", "\n\n");
        
        return text;
    }
    
    /**
     * 初步分段处理
     */
    private static List<String> initialSegmentation(String text) {
        List<String> segments = new ArrayList<>();
        
        // 按空行分割成初步段落
        String[] blocks = text.split("\n\n+");
        
        for (String block : blocks) {
            // 进一步处理每个块
            processTextBlock(block.trim(), segments);
        }
        
        return segments;
    }
    
    /**
     * 处理文本块，进一步细分
     */
    private static void processTextBlock(String block, List<String> segments) {
        if (block.isEmpty()) {
            return;
        }
        
        // 检查是否是代码块
        if (isLikelyCodeBlock(block)) {
            segments.add(block);
            return;
        }
        
        // 按行分割
        String[] lines = block.split("\n");
        
        // 如果只有一行，直接添加
        if (lines.length == 1) {
            segments.add(block);
            return;
        }
        
        // 处理多行文本块
        StringBuilder currentSegment = new StringBuilder();
        boolean inNumberedList = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }
            
            // 检查是否是编号条目的开始
            boolean isNumberedItem = NUMBERED_ITEM_PATTERN.matcher(line).matches();
            
            // 如果当前行是编号条目且前面已有内容，保存前面的内容并开始新段落
            if (isNumberedItem && currentSegment.length() > 0) {
                segments.add(currentSegment.toString().trim());
                currentSegment = new StringBuilder();
                inNumberedList = true;
            }
            
            // 添加当前行到段落
            if (currentSegment.length() > 0) {
                currentSegment.append(" ");
            }
            currentSegment.append(line);
            
            // 如果是最后一行或下一行是编号条目的开始，保存当前段落
            if (i == lines.length - 1 || 
                (i < lines.length - 1 && NUMBERED_ITEM_PATTERN.matcher(lines[i + 1].trim()).matches())) {
                segments.add(currentSegment.toString().trim());
                currentSegment = new StringBuilder();
            }
        }
        
        // 处理最后一个段落
        if (currentSegment.length() > 0) {
            segments.add(currentSegment.toString().trim());
        }
    }
    
    /**
     * 合并相关段落和短段落
     */
    private static List<String> mergeRelatedSegments(List<String> segments, int maxLength) {
        if (segments.size() <= 1) {
            return segments;
        }
        
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder(segments.get(0));
        
        for (int i = 1; i < segments.size(); i++) {
            String segment = segments.get(i);
            
            // 检查是否应该合并
            boolean shouldMerge = false;
            
            // 条件1: 当前段落很短
            if (current.length() < MIN_PARAGRAPH_LENGTH || segment.length() < MIN_PARAGRAPH_LENGTH) {
                shouldMerge = true;
            }
            
            // 条件2: 当前段落是上一段的继续（如示例、解释等）
            if (isRelatedContent(current.toString(), segment)) {
                shouldMerge = true;
            }
            
            // 条件3: 合并后不超过最大长度的一半，优先保持段落完整性
            if (current.length() + segment.length() + 1 <= MIN_MERGE_LENGTH) {
                shouldMerge = true;
            }
            
            // 如果应该合并且合并后不超过最大长度
            if (shouldMerge && current.length() + segment.length() + 1 <= maxLength) {
                current.append("\n").append(segment);
            } else {
                // 保存当前段落，开始新段落
                result.add(current.toString());
                current = new StringBuilder(segment);
            }
        }
        
        // 添加最后一个段落
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    /**
     * 处理过长段落
     */
    private static List<String> handleLongSegments(List<String> segments, int maxLength) {
        List<String> result = new ArrayList<>();
        
        for (String segment : segments) {
            if (segment.length() <= maxLength) {
                result.add(segment);
            } else {
                // 尝试按句子分割
                List<String> splitResult = splitByNaturalBreaks(segment, maxLength);
                result.addAll(splitResult);
            }
        }
        
        return result;
    }
    
    /**
     * 按自然断点（句号、问号、感叹号等）分割长文本
     */
    private static List<String> splitByNaturalBreaks(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        
        // 定义句子结束的模式
        Pattern sentenceEndPattern = Pattern.compile("([.。!！?？;；]\\s*)");
        
        StringBuilder current = new StringBuilder();
        int lastEnd = 0;
        Matcher matcher = sentenceEndPattern.matcher(text);
        
        while (matcher.find()) {
            int end = matcher.end();
            String sentence = text.substring(lastEnd, end);
            
            if (current.length() + sentence.length() <= maxLength) {
                current.append(sentence);
            } else {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder(sentence);
                } else {
                    // 单个句子超过最大长度，强制切分
                    result.add(sentence);
                }
            }
            
            lastEnd = end;
        }
        
        // 处理最后一部分
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            if (current.length() + remaining.length() <= maxLength) {
                current.append(remaining);
            } else {
                if (current.length() > 0) {
                    result.add(current.toString());
                }
                // 按最大长度切分剩余部分
                for (int i = 0; i < remaining.length(); i += maxLength) {
                    int end = Math.min(i + maxLength, remaining.length());
                    result.add(remaining.substring(i, end));
                }
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    /**
     * 判断文本是否可能是代码块
     */
    private static boolean isLikelyCodeBlock(String text) {
        // 包含常见代码特征
        if (text.contains("public ") || text.contains("private ") || 
            text.contains("class ") || text.contains("interface ") ||
            text.contains("import ") || text.contains("package ")) {
            return true;
        }
        
        // 包含多个大括号、分号等代码特征
        int braceCount = countOccurrences(text, '{') + countOccurrences(text, '}');
        int semicolonCount = countOccurrences(text, ';');
        
        return braceCount >= 2 || semicolonCount >= 3;
    }
    
    /**
     * 判断两段内容是否相关（如正例/反例、规则/说明等）
     */
    private static boolean isRelatedContent(String prev, String current) {
        // 检查常见的关联模式
        if (prev.contains("正例：") && current.contains("反例：")) {
            return true;
        }
        
        if (prev.contains("说明：") || current.contains("说明：")) {
            return true;
        }
        
        if (prev.contains("例如：") || current.contains("例如：")) {
            return true;
        }
        
        // 检查是否是同一编号下的内容
        Pattern numberPattern = Pattern.compile("^\\s*(\\d+\\.|[一二三四五六七八九十]+、)");
        Matcher prevMatcher = numberPattern.matcher(prev);
        Matcher currentMatcher = numberPattern.matcher(current);
        
        if (prevMatcher.find() && !currentMatcher.find()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 计算字符在文本中出现的次数
     */
    private static int countOccurrences(String text, char c) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 按最大长度切分文本为段落（简单切分，用于备选）
     *
     * @param text      文本内容
     * @param maxLength 每段最大长度
     * @return 切分后的段落列表
     */
    public static List<String> split(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += maxLength) {
            int end = Math.min(i + maxLength, length);
            result.add(text.substring(i, end));
        }
        return result;
    }
} 