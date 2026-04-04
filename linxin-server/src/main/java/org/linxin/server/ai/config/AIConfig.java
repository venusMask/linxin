package org.linxin.server.ai.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AIConfig {
    // 基础单点配置 (用于向后兼容)
    private String baseUrl;
    private String apiKey;
    private String model;
    private Double temperature = 0.7;

    // 多 Provider 配置 (新增核心)
    private List<AIProviderConfig> providers = new ArrayList<>();
}
