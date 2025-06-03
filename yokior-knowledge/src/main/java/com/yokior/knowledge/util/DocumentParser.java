package com.yokior.knowledge.util;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.File;
import java.io.IOException;

/**
 * 文档解析工具类
 */
public class DocumentParser
{

    /**
     * 解析文档内容为文本
     *
     * @param filePath 文件路径
     * @return 解析后的文本内容
     * @throws IOException   IO异常
     * @throws TikaException Tika解析异常
     */
    public static String parse(String filePath) throws IOException, TikaException
    {
        Tika tika = new Tika();
        return tika.parseToString(new File(filePath));
    }

    /**
     * 解析文档内容为文本
     *
     * @param file 文件对象
     * @return 解析后的文本内容
     * @throws IOException   IO异常
     * @throws TikaException Tika解析异常
     */
    public static String parse(File file) throws IOException, TikaException
    {
        Tika tika = new Tika();
        return tika.parseToString(file);
    }
} 