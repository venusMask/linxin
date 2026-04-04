package org.linxin.server.business.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.business.entity.Friend;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.service.IAgentService;
import org.linxin.server.business.service.IFriendService;
import org.linxin.server.business.service.IMessageService;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final IFriendService friendService;
    private final IMessageService messageService;

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
        if ("send_message".equals(toolName)) {
            return handleSendMessage(userId, arguments, agentName);
        }
        return errorResponse("UNKNOWN_TOOL", "不支持的工具: " + toolName);
    }

    private Map<String, Object> handleSendMessage(Long userId, Map<String, Object> arguments, String agentName) {
        String recipient = (String) arguments.get("recipient");
        String content = (String) arguments.get("content");

        if (recipient == null || content == null) {
            return errorResponse("INVALID_ARGUMENTS", "缺少必要参数: recipient 或 content");
        }

        // 语义对齐：Resolver 逻辑
        List<Friend> candidates = friendService.resolveRecipient(userId, recipient);

        if (candidates.isEmpty()) {
            return errorResponse("NOT_FOUND", "未找到匹配联系人: " + recipient);
        }

        if (candidates.size() > 1) {
            List<Map<String, String>> choices = new ArrayList<>();
            for (Friend f : candidates) {
                choices.add(Map.of(
                    "hint", f.getFriendNickname() + (f.getTags() != null ? " (" + f.getTags() + ")" : ""),
                    "id_placeholder", f.getFriendId().toString()
                ));
            }
            Map<String, Object> res = errorResponse("AMBIGUOUS_RECIPIENT", "发现多个匹配项，请确认：");
            res.put("choices", choices);
            return res;
        }

        // 执行发送
        Long targetId = candidates.get(0).getFriendId();
        Message msg = messageService.sendAgentMessage(userId, targetId, content, agentName);

        Map<String, Object> success = new HashMap<>();
        success.put("status", "SUCCESS");
        success.put("data", Map.of(
            "target", candidates.get(0).getFriendNickname(),
            "sequenceId", msg.getSequenceId()
        ));
        return success;
    }

    @Override
    public Map<String, Object> executeCommand(Long userId, String command, String agentName) {
        // 保留旧接口用于兼容，但内部逻辑应重构为调用 LLM 进行意图解析后再调 callTool
        // 这里暂时实现为提示用户
        return errorResponse("DEPRECATED", "请使用结构化的 /api/agent/call 接口，或在 App 内直接与 AI 对话");
    }

    private Map<String, Object> errorResponse(String status, String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", status);
        res.put("message", message);
        return res;
    }
}
