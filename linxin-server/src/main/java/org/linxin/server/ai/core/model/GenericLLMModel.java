package org.linxin.server.ai.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.core.dto.ChatMessage;
import org.linxin.server.ai.core.dto.ModelResponse;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class GenericLLMModel implements LLMModel {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String providerName;
    private final double temperature;
    private final ObjectMapper objectMapper;

    public GenericLLMModel(String providerName, String baseUrl, String apiKey, String model, double temperature) {
        this.restTemplate = new RestTemplate();
        this.providerName = providerName;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ModelResponse chat(List<ChatMessage> messages, List<Map<String, Object>> tools) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);

            if (tools != null && !tools.isEmpty()) {
                requestBody.put("tools", tools);
                requestBody.put("tool_choice", "auto");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling AI [{}]: {} with {} messages", providerName, model, messages.size());

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                return parseResponse(responseMap);
            }

            throw new RuntimeException("AI服务异常 (" + response.getStatusCode() + "): " + response.getBody());
        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage());
            throw new RuntimeException("AI Model invocation failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private ModelResponse parseResponse(Map<String, Object> responseMap) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            return ModelResponse.builder().content("AI未返回任何内容").build();
        }

        Map<String, Object> messageMap = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) messageMap.get("content");
        List<Map<String, Object>> toolCallsMap = (List<Map<String, Object>>) messageMap.get("tool_calls");

        List<ChatMessage.ToolCall> toolCalls = null;
        if (toolCallsMap != null) {
            toolCalls = toolCallsMap.stream().map(tc -> {
                Map<String, Object> func = (Map<String, Object>) tc.get("function");
                return ChatMessage.ToolCall.builder()
                        .id((String) tc.get("id"))
                        .type("function")
                        .function(ChatMessage.Function.builder()
                                .name((String) func.get("name"))
                                .arguments((String) func.get("arguments"))
                                .build())
                        .build();
            }).collect(Collectors.toList());
        }

        Map<String, Object> usageMap = (Map<String, Object>) responseMap.get("usage");
        ModelResponse.Usage usage = null;
        if (usageMap != null) {
            usage = ModelResponse.Usage.builder()
                    .promptTokens((int) usageMap.getOrDefault("prompt_tokens", 0))
                    .completionTokens((int) usageMap.getOrDefault("completion_tokens", 0))
                    .totalTokens((int) usageMap.getOrDefault("total_tokens", 0))
                    .build();
        }

        return ModelResponse.builder()
                .content(content)
                .toolCalls(toolCalls)
                .usage(usage)
                .build();
    }

    @Override
    public String getProviderName() {
        return providerName;
    }
    @Override
    public String getModelName() {
        return model;
    }
}
