package org.venus.lin.xin.mgr.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AIConfig {
    private String baseUrl;
    private String apiKey;
    private String model;
    private Double temperature;
}