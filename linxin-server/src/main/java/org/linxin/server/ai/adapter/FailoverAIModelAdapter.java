package org.linxin.server.ai.adapter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.tools.AITool;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
public class FailoverAIModelAdapter implements AIModelAdapter {

    private final List<AIModelAdapter> adapters;
    private int currentActiveIndex = 0;

    public FailoverAIModelAdapter(List<AIModelAdapter> adapters) {
        this.adapters = adapters;
    }

    @Override
    public String getProviderName() {
        if (adapters == null || adapters.isEmpty())
            return "Unknown";
        return adapters.get(currentActiveIndex).getProviderName();
    }

    @Override
    public String getModelName() {
        if (adapters == null || adapters.isEmpty())
            return "Unknown";
        return adapters.get(currentActiveIndex).getModelName();
    }

    @Override
    public ChatResponse chat(String userInput, List<AITool> tools) {
        if (adapters == null || adapters.isEmpty()) {
            return createErrorResponse("未配置任何有效的AI模型适配器");
        }

        Exception lastException = null;
        for (int i = 0; i < adapters.size(); i++) {
            AIModelAdapter adapter = adapters.get(i);
            try {
                log.info("尝试使用 AI 适配器 [{}]: {}", i, adapter.getProviderName());
                ChatResponse response = adapter.chat(userInput, tools);

                if ("error".equals(response.getIntent()) && isQuotaExceeded(response.getAiText())) {
                    log.warn("适配器 {} 额度已用尽，准备切换...", i);
                    continue;
                }

                this.currentActiveIndex = i;
                return response;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == org.springframework.http.HttpStatus.TOO_MANY_REQUESTS ||
                        e.getStatusCode() == org.springframework.http.HttpStatus.PAYMENT_REQUIRED) {
                    log.warn("AI 适配器 {} 额度超限: {}", i, e.getMessage());
                    continue; // 切换下一个
                }
                if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN) {
                    String body = e.getResponseBodyAsString();
                    if (isQuotaExceeded(body)) {
                        log.warn("AI 适配器 {} 触发配额限制: {}", i, body);
                        continue;
                    }
                }
                log.error("AI 适配器 {} 发生 HTTP 错误: {}", i, e.getMessage());
                lastException = e;
            } catch (ResourceAccessException e) {
                log.warn("AI 适配器 {} 连接超时", i);
                lastException = e;
            } catch (Exception e) {
                log.error("AI 适配器 {} 未知错误", i, e);
                lastException = e;
            }
        }

        return createErrorResponse("所有配置的AI模型均不可用: " + (lastException != null ? lastException.getMessage() : "none"));
    }

    private boolean isQuotaExceeded(String text) {
        if (text == null)
            return false;
        String lower = text.toLowerCase();
        return lower.contains("quota") || lower.contains("balance") || lower.contains("insufficient")
                || lower.contains("allocationquota") || lower.contains("额度不足") || lower.contains("余额不足");
    }

    private ChatResponse createErrorResponse(String errorMessage) {
        ChatResponse errorResponse = new ChatResponse();
        errorResponse.setIntent("error");
        errorResponse.setAiText(errorMessage);
        return errorResponse;
    }

    @Override
    public void dispose() {
        for (AIModelAdapter adapter : adapters)
            adapter.dispose();
    }
}
