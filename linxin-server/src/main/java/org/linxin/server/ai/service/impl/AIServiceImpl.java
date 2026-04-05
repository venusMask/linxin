package org.linxin.server.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.core.agent.AIAgent;
import org.linxin.server.ai.core.dto.ModelResponse;
import org.linxin.server.ai.core.model.LLMModel;
import org.linxin.server.ai.core.tool.ToolProvider;
import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.service.AIService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIAgent aiAgent;
    private final ToolProvider toolProvider;
    private final LLMModel activeModel;
    private final org.linxin.server.ai.mapper.AIUsageLogMapper usageLogMapper;

    @Override
    public ChatResponse processUserInput(ChatRequest request,
            java.util.List<org.linxin.server.ai.core.dto.ChatMessage> previousMessages) {
        log.info("Processing multi-step AI request for user: {}", request.getUserId());

        ModelResponse agentResult = aiAgent.run(request.getUserId(), request.getContent(), previousMessages);

        // 转换为旧的 DTO 结构以兼容前端
        ChatResponse response = new ChatResponse();
        response.setIntent(agentResult.hasToolCalls() ? "tool_call" : "chat");
        response.setAiText(agentResult.getContent());
        response.setReply(agentResult.getContent());

        if (agentResult.getUsage() != null) {
            ChatResponse.Usage legacyUsage = new ChatResponse.Usage();
            legacyUsage.setPromptTokens(agentResult.getUsage().getPromptTokens());
            legacyUsage.setCompletionTokens(agentResult.getUsage().getCompletionTokens());
            legacyUsage.setTotalTokens(agentResult.getUsage().getTotalTokens());
            response.setUsage(legacyUsage);

            // 异步记录日志
            logUsage(request.getUserId(), agentResult.getUsage());
        }

        return response;
    }

    @Async("taskExecutor")
    protected void logUsage(Long userId, ModelResponse.Usage usage) {
        try {
            org.linxin.server.ai.entity.AIUsageLog logEntry = new org.linxin.server.ai.entity.AIUsageLog();
            logEntry.setUserId(userId);
            logEntry.setProviderName(activeModel.getProviderName());
            logEntry.setModelName(activeModel.getModelName());
            logEntry.setIntent("agent_workflow");
            logEntry.setPromptTokens(usage.getPromptTokens());
            logEntry.setCompletionTokens(usage.getCompletionTokens());
            logEntry.setTotalTokens(usage.getTotalTokens());
            usageLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("Failed to log AI usage", e);
        }
    }

    @Override
    public ChatResponse modifyParams(ChatResponse originalResponse, String modification) {
        return originalResponse;
    }

    @Override
    public String getToolsVersion() {
        return "2.0.0-Agent";
    }

    @Override
    public java.util.Map<String, Object> getUsageStatistics(Long userId, Integer days) {
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now().minusDays(days != null ? days : 3);
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("daily", usageLogMapper.selectDailyUsage(userId, startTime));
        stats.put("intents", usageLogMapper.selectIntentUsage(userId, startTime));
        return stats;
    }
}
