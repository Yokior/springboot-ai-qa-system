package com.yokior.ai.domain;

import lombok.Data;

@Data
public class ProviderConfig
{
    private String apiKey;
    private String endpoint;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Boolean stream;
    private Double topP;
}