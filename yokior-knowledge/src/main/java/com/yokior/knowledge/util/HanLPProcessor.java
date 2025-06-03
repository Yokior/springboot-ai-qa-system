package com.yokior.knowledge.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HanLP分词处理工具类
 */
public class HanLPProcessor
{

    /**
     * 常用停用词集合
     */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "有", "我", "他", "她", "它", "和", "就", "都",
            "这", "那", "你", "我们", "他们", "她们", "它们", "什么", "怎么", "为什么",
            "如何", "可以", "因为", "所以", "但是", "而且", "如果", "虽然", "即使",
            "不", "没", "很", "太", "非常", "更", "最", "又", "也", "还", "只", "就",
            "才", "刚", "曾", "已", "将", "会", "能", "可能", "应该", "必须", "需要"
    ));

    /**
     * 对文本进行分词，并过滤停用词
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<String> segment(String text)
    {
        List<String> words = new ArrayList<>();
        for (Term term : HanLP.segment(text))
        {
            String word = term.word.trim();
            if (!STOPWORDS.contains(word) && word.length() > 1)
            {
                words.add(word);
            }
        }
        return words;
    }

    /**
     * 对文本进行分词，不过滤停用词
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<String> segmentWithStopwords(String text)
    {
        List<String> words = new ArrayList<>();
        for (Term term : HanLP.segment(text))
        {
            String word = term.word.trim();
            if (!word.isEmpty())
            {
                words.add(word);
            }
        }
        return words;
    }

    /**
     * 将分词结果转换为JSON字符串形式
     *
     * @param terms 分词结果列表
     * @return JSON字符串，格式为["词1","词2",...]
     */
    public static String termsToJsonString(List<String> terms)
    {
        if (terms == null || terms.isEmpty())
        {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < terms.size(); i++)
        {
            if (i > 0)
            {
                sb.append(",");
            }
            sb.append("\"").append(terms.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
} 