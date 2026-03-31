package org.venus.lin.xin.mgr.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.venus.lin.xin.mgr.ai.adapter.AIModelAdapter;
import org.venus.lin.xin.mgr.ai.adapter.OpenAIAdapter;
import org.venus.lin.xin.mgr.ai.config.AIConfig;
import org.venus.lin.xin.mgr.ai.dto.ChatRequest;
import org.venus.lin.xin.mgr.ai.dto.ChatResponse;
import org.venus.lin.xin.mgr.ai.service.AIService;
import org.venus.lin.xin.mgr.ai.tools.AITool;
import org.venus.lin.xin.mgr.ai.tools.ToolsConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AIService {

    private final ObjectMapper objectMapper;
    private final AIConfig aiConfig;

    private AIModelAdapter adapter;
    private List<AITool> tools = new ArrayList<>();
    private String toolsVersion = "1.0.0";
    private static final int MAX_HISTORY_PER_USER = 20;
    private static final int HISTORY_EXPIRE_HOURS = 24;
    private final Map<Long, Deque<ChatResponse>> userConversationHistory = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> userLastActiveTime = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadTools();
        initAdapter();
    }

    private void loadTools() {
        try {
            ClassPathResource resource = new ClassPathResource("tools/default_tools.json");
            String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ToolsConfig config = objectMapper.readValue(json, ToolsConfig.class);
            this.tools = config.getTools();
            this.toolsVersion = config.getVersion();
            log.info("Loaded {} AI tools, version: {}", tools.size(), toolsVersion);
        } catch (IOException e) {
            log.error("Failed to load AI tools from config", e);
            tools = getDefaultTools();
            toolsVersion = "1.0.0";
        }
    }

    private void initAdapter() {
        adapter = new OpenAIAdapter(
            aiConfig.getBaseUrl(),
            aiConfig.getApiKey(),
            aiConfig.getModel(),
            aiConfig.getTemperature()
        );
    }

    @Override
    public ChatResponse processUserInput(ChatRequest request) {
        Long userId = request.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        String userInput = request.getUserInput();

        ChatResponse response = adapter.chat(userInput, tools);

        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            for (ChatResponse.ToolCall call : response.getToolCalls()) {
                fillToolDescription(call);
            }
        }

        userConversationHistory.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>()).add(response);
        userLastActiveTime.put(userId, LocalDateTime.now());
        
        Deque<ChatResponse> history = userConversationHistory.get(userId);
        while (history.size() > MAX_HISTORY_PER_USER) {
            history.pollFirst();
        }

        return response;
    }

    @Override
    public ChatResponse modifyParams(ChatResponse originalResponse, String modification) {
        String currentState = formatCurrentState(originalResponse);
        String modifiedInput = String.format(
                "当前待执行的操作：\n%s\n\n用户修改意见：%s\n\n请根据用户的修改意见，更新参数，返回JSON格式：",
                currentState, modification
        );

        ChatResponse response = adapter.chat(modifiedInput, tools);

        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            for (ChatResponse.ToolCall call : response.getToolCalls()) {
                fillToolDescription(call);
            }
            response.setStatus("modified");
        }

        return response;
    }

    @Override
    public List<AITool> getAvailableTools() {
        return new ArrayList<>(tools);
    }

    @Override
    public String getToolsVersion() {
        return toolsVersion;
    }

    private void fillToolDescription(ChatResponse.ToolCall call) {
        AITool tool = tools.stream()
                .filter(t -> t.getName().equals(call.getToolName()))
                .findFirst()
                .orElse(null);

        if (tool != null) {
            StringBuilder desc = new StringBuilder(tool.getDescription());
            if (call.getParams() != null && !call.getParams().isEmpty()) {
                desc.append(" | ");
                boolean first = true;
                for (Map.Entry<String, Object> entry : call.getParams().entrySet()) {
                    if (!first) desc.append(", ");
                    String paramDesc = getParamDescription(tool, entry.getKey());
                    desc.append(paramDesc).append(": ").append(entry.getValue());
                    first = false;
                }
            }
            call.setDescription(desc.toString());
        }
    }

    private String getParamDescription(AITool tool, String paramName) {
        if (tool.getParams() == null) return paramName;
        return tool.getParams().stream()
                .filter(p -> p.getName().equals(paramName))
                .findFirst()
                .map(p -> p.getDescription())
                .orElse(paramName);
    }

    private String formatCurrentState(ChatResponse response) {
        if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
            return "无待执行操作";
        }

        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (ChatResponse.ToolCall call : response.getToolCalls()) {
            sb.append(String.format("操作 %d: %s\n", index++, call.getDescription()));
        }
        return sb.toString();
    }

    private List<AITool> getDefaultTools() {
        List<AITool> defaultTools = new ArrayList<>();

        AITool sendMessage = new AITool();
        sendMessage.setId("send_message");
        sendMessage.setName("sendMessage");
        sendMessage.setDescription("发送消息给指定用户");
        sendMessage.setRequireConfirm(true);
        defaultTools.add(sendMessage);

        AITool createGroup = new AITool();
        createGroup.setId("create_group");
        createGroup.setName("createGroup");
        createGroup.setDescription("创建群聊并添加成员");
        createGroup.setRequireConfirm(true);
        defaultTools.add(createGroup);

        AITool addFriend = new AITool();
        addFriend.setId("add_friend");
        addFriend.setName("addFriend");
        addFriend.setDescription("申请添加指定用户为好友");
        addFriend.setRequireConfirm(true);
        defaultTools.add(addFriend);

        return defaultTools;
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredHistory() {
        LocalDateTime expireTime = LocalDateTime.now().minus(HISTORY_EXPIRE_HOURS, ChronoUnit.HOURS);
        
        Iterator<Map.Entry<Long, LocalDateTime>> iterator = userLastActiveTime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, LocalDateTime> entry = iterator.next();
            if (entry.getValue().isBefore(expireTime)) {
                Long userId = entry.getKey();
                iterator.remove();
                userConversationHistory.remove(userId);
                log.debug("Cleaned up expired history for user: {}", userId);
            }
        }
        
        log.info("History cleanup completed. Active users: {}", userLastActiveTime.size());
    }

    @PreDestroy
    public void destroy() {
        userConversationHistory.clear();
        userLastActiveTime.clear();
        log.info("AI Service destroyed, all history cleared");
    }
}