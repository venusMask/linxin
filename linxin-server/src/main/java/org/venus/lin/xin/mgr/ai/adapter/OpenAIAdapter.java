package org.venus.lin.xin.mgr.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.venus.lin.xin.mgr.ai.dto.ChatResponse;
import org.venus.lin.xin.mgr.ai.tools.AITool;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class OpenAIAdapter implements AIModelAdapter {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final ObjectMapper objectMapper;

    public OpenAIAdapter(String baseUrl, String apiKey, String model, double temperature) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatResponse chat(String userInput, List<AITool> tools) {
        try {
            String systemPrompt = buildSystemPrompt(tools);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userInput));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("tools", convertToolsToOpenAIFormat(tools));
            requestBody.put("temperature", temperature);
            requestBody.put("tool_choice", "auto");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                return parseResponse(responseMap);
            }

            return createErrorResponse("AI服务返回异常状态码: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("OpenAI chat failed", e);
            return createErrorResponse("AI服务调用失败: " + e.getMessage());
        }
    }

    private String buildSystemPrompt(List<AITool> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个即时通讯AI助手，用户通过自然语言描述操作意图。\n");
        sb.append("你必须解析用户意图并返回JSON格式的tool_call。\n\n");
        sb.append("可用tools（JSON格式）：\n");
        try {
            sb.append(objectMapper.writeValueAsString(tools));
        } catch (Exception e) {
            log.error("Failed to serialize tools", e);
        }
        sb.append("\n\n返回格式（必须是有效JSON）：\n");
        sb.append("{\n");
        sb.append("  \"intent\": \"操作意图名称\",\n");
        sb.append("  \"toolCalls\": [{\"toolId\": \"tool的id\", \"toolName\": \"tool的name\", \"params\": {}, \"description\": \"描述\"}],\n");
        sb.append("  \"aiText\": \"对用户的确认提示语\",\n");
        sb.append("  \"needConfirm\": true\n");
        sb.append("}\n\n");
        sb.append("重要规则：\n");
        sb.append("1. 必须返回有效的JSON\n");
        sb.append("2. intent使用英文驼峰命名：sendMessage, createGroup, addFriend等\n");
        sb.append("3. toolName必须与可用tools中的name完全一致\n");
        sb.append("4. 如果无法解析用户意图，返回空toolCalls和解释文字\n");
        sb.append("5. 只有执行操作时才需要确认(needConfirm=true)\n");
        return sb.toString();
    }

    private List<Map<String, Object>> convertToolsToOpenAIFormat(List<AITool> tools) {
        return tools.stream().map(tool -> {
            Map<String, Object> function = new HashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());

            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();

            if (tool.getParams() != null) {
                for (var param : tool.getParams()) {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("type", param.getType());
                    paramMap.put("description", param.getDescription());
                    if (param.getEnumValues() != null) {
                        paramMap.put("enum", param.getEnumValues());
                    }
                    properties.put(param.getName(), paramMap);
                    if (param.isRequired()) {
                        required.add(param.getName());
                    }
                }
            }

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", properties);
            parameters.put("required", required);

            function.put("parameters", parameters);

            Map<String, Object> result = new HashMap<>();
            result.put("type", "function");
            result.put("function", function);
            return result;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private ChatResponse parseResponse(Map<String, Object> responseMap) {
        ChatResponse chatResponse = new ChatResponse();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            chatResponse.setIntent("chat");
            chatResponse.setToolCalls(Collections.emptyList());
            chatResponse.setAiText("AI无响应");
            chatResponse.setNeedConfirm(false);
            return chatResponse;
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            List<ChatResponse.ToolCall> calls = new ArrayList<>();
            for (Map<String, Object> call : toolCalls) {
                Map<String, Object> function = (Map<String, Object>) call.get("function");
                String argumentsStr = (String) function.get("arguments");

                Map<String, Object> params = new HashMap<>();
                if (argumentsStr != null && !argumentsStr.isEmpty()) {
                    try {
                        params = objectMapper.readValue(argumentsStr, Map.class);
                    } catch (Exception e) {
                        log.warn("Failed to parse arguments: {}", argumentsStr);
                    }
                }

                ChatResponse.ToolCall toolCall = new ChatResponse.ToolCall();
                toolCall.setToolId("");
                toolCall.setToolName((String) function.get("name"));
                toolCall.setParams(params);
                toolCall.setDescription("");
                calls.add(toolCall);
            }

            chatResponse.setIntent("tool_call");
            chatResponse.setToolCalls(calls);
            chatResponse.setAiText(content != null ? content : "即将执行 " + calls.size() + " 个操作");
            chatResponse.setNeedConfirm(true);
        } else {
            chatResponse.setIntent("chat");
            chatResponse.setToolCalls(Collections.emptyList());
            chatResponse.setAiText(content != null ? content : "");
            chatResponse.setNeedConfirm(false);
        }

        return chatResponse;
    }

    private ChatResponse createErrorResponse(String errorMessage) {
        ChatResponse errorResponse = new ChatResponse();
        errorResponse.setIntent("error");
        errorResponse.setToolCalls(Collections.emptyList());
        errorResponse.setAiText(errorMessage);
        errorResponse.setNeedConfirm(false);
        return errorResponse;
    }

    @Override
    public void dispose() {
    }
}