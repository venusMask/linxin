package org.linxin.server.ai.service.impl;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.adapter.AIModelAdapter;
import org.linxin.server.ai.adapter.FailoverAIModelAdapter;
import org.linxin.server.ai.adapter.OpenAIAdapter;
import org.linxin.server.ai.config.AIConfig;
import org.linxin.server.ai.config.AIProviderConfig;
import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.service.AIService;
import org.linxin.server.ai.tools.AITool;
import org.linxin.server.ai.tools.ToolsConfig;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIConfig aiConfig;
    private final ToolsConfig toolsConfig;
    private final org.linxin.server.ai.mapper.AIUsageLogMapper usageLogMapper;
    private AIModelAdapter activeAdapter;

    @PostConstruct
    public void init() {
        List<AIModelAdapter> adapters = new ArrayList<>();

        // 1. 处理多 Provider 配置
        List<AIProviderConfig> providers = aiConfig.getProviders();
        if (providers != null && !providers.isEmpty()) {
            List<AIProviderConfig> sortedProviders = providers.stream()
                    .sorted(Comparator.comparing(AIProviderConfig::getPriority).reversed())
                    .toList();

            for (AIProviderConfig p : sortedProviders) {
                adapters.add(new OpenAIAdapter(p.getName(), p.getBaseUrl(), p.getApiKey(), p.getModel(),
                        p.getTemperature()));
                log.info("加载 AI Provider: {}", p.getName());
            }
        }

        // 2. 向后兼容处理单点配置
        if (aiConfig.getBaseUrl() != null && !aiConfig.getBaseUrl().isEmpty()) {
            adapters.add(new OpenAIAdapter("Default", aiConfig.getBaseUrl(), aiConfig.getApiKey(), aiConfig.getModel(),
                    aiConfig.getTemperature()));
        }

        this.activeAdapter = new FailoverAIModelAdapter(adapters);
    }

    @Override
    public ChatResponse processUserInput(ChatRequest request) {
        ChatResponse response = activeAdapter.chat(request.getContent(), toolsConfig.getTools());

        // 异步记录消耗日志
        if (response.getUsage() != null) {
            final Long userId = request.getUserId();
            final String provider = activeAdapter.getProviderName();
            final String model = activeAdapter.getModelName();
            final String intent = response.getIntent();
            final ChatResponse.Usage usage = response.getUsage();

            new Thread(() -> {
                try {
                    org.linxin.server.ai.entity.AIUsageLog logEntry = new org.linxin.server.ai.entity.AIUsageLog();
                    logEntry.setUserId(userId);
                    logEntry.setProviderName(provider);
                    logEntry.setModelName(model);
                    logEntry.setIntent(intent);
                    logEntry.setPromptTokens(usage.getPromptTokens());
                    logEntry.setCompletionTokens(usage.getCompletionTokens());
                    logEntry.setTotalTokens(usage.getTotalTokens());
                    usageLogMapper.insert(logEntry);
                } catch (Exception e) {
                    log.error("Failed to log AI usage", e);
                }
            }).start();
        }

        return response;
    }

    @Override
    public ChatResponse modifyParams(ChatResponse originalResponse, String modification) {
        return originalResponse;
    }

    @Override
    public List<AITool> getAvailableTools() {
        return toolsConfig.getTools();
    }

    @Override
    public String getToolsVersion() {
        return toolsConfig.getVersion();
    }
}
