package org.linxin.server.business.service.impl;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.handler.AIToolHandler;
import org.linxin.server.business.service.IAgentService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final List<AIToolHandler> handlers;
    private final Map<String, AIToolHandler> handlerMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (AIToolHandler handler : handlers) {
            handlerMap.put(handler.getToolName(), handler);
            log.info("Registered AI Tool Handler: {}", handler.getToolName());
        }
    }

    @Override
    public Map<String, Object> getManifest() {
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("version", "1.0.0");

        List<Map<String, Object>> tools = new ArrayList<>();

        Map<String, Object> sendMessage = new HashMap<>();
        sendMessage.put("name", "send_message");
        sendMessage.put("description", "发送文本消息。支持模糊匹配联系人。");

        Map<String, Object> params = new HashMap<>();
        params.put("recipient", "string (目标联系人名称/备注/语义描述如'媳妇')");
        params.put("content", "string (消息文本)");

        sendMessage.put("parameters", params);
        tools.add(sendMessage);

        manifest.put("tools", tools);
        return manifest;
    }

    @Override
    public Map<String, Object> callTool(Long userId, String toolName, Map<String, Object> arguments, String agentName) {
        // 将驼峰转换或规范化 toolName (例如 sendMessage -> send_message)
        String normalizedName = normalizeToolName(toolName);
        AIToolHandler handler = handlerMap.get(normalizedName);

        if (handler != null) {
            log.info("Executing AI Tool: {} for user {}", normalizedName, userId);
            return handler.execute(userId, arguments);
        }

        return errorResponse("UNKNOWN_TOOL", "不支持的工具: " + toolName);
    }

    private String normalizeToolName(String name) {
        if (name == null)
            return "";
        // 处理驼峰命名转下划线 (例如 createGroup -> create_group)
        String snaked = name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        // 处理可能存在的多个下划线
        return snaked.replace("__", "_");
    }

    @Override
    public Map<String, Object> executeCommand(Long userId, String command, String agentName) {
        // 保留旧接口用于兼容
        return errorResponse("DEPRECATED", "请使用结构化的 /api/agent/call 接口，或在 App 内直接与 AI 对话");
    }

    private Map<String, Object> errorResponse(String status, String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", status);
        res.put("message", message);
        return res;
    }
}
