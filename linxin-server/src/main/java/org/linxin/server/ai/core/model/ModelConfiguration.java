package org.linxin.server.ai.core.model;

import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ModelConfiguration {

    @Bean
    @Primary
    public LLMModel primaryModel(AIConfig aiConfig) {
        List<AIProviderConfig> providers = aiConfig.getProviders();
        if (providers == null || providers.isEmpty()) {
            // 回退到基础配置
            return new GenericLLMModel("Default", aiConfig.getBaseUrl(), aiConfig.getApiKey(), aiConfig.getModel(),
                    aiConfig.getTemperature());
        }

        // 选择优先级最高的 Provider
        AIProviderConfig p = providers.stream()
                .sorted(Comparator.comparing(AIProviderConfig::getPriority).reversed())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No AI providers configured"));

        return new GenericLLMModel(p.getName(), p.getBaseUrl(), p.getApiKey(), p.getModel(), p.getTemperature());
    }
}
