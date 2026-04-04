package org.linxin.server.ai.config;

import lombok.Data;

@Data
public class AIProviderConfig {
    private String name;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Double temperature = 0.7;
    private Integer priority = 0; // 优先级，越高越先使用
}
