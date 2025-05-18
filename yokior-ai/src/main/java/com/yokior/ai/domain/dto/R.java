package com.yokior.ai.domain.dto;

import lombok.Data;

// 通用API响应包装类
@Data
public class R<T>
{
    private Integer code;
    private String msg;
    private T data;
}