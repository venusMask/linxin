package org.linxin.server.ai.handler.impl;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.handler.AIToolHandler;
import org.linxin.server.module.chat.entity.Message;
import org.linxin.server.module.chat.service.IMessageService;
import org.linxin.server.module.contact.entity.Friend;
import org.linxin.server.module.contact.service.IFriendService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageHandler implements AIToolHandler {

    private final IFriendService friendService;
    private final IMessageService messageService;
    private final org.linxin.server.module.user.mapper.UserMapper userMapper;

    @Override
    public String getToolName() {
        return "send_message";
    }

    @Override
    public Map<String, Object> execute(Long userId, Map<String, Object> arguments) {
        Object recipientObj = arguments.get("recipient");
        if (recipientObj == null) {
            recipientObj = arguments.get("receiverId"); // 兼容字段名
        }
        String recipient = recipientObj != null ? recipientObj.toString() : null;
        String content = (String) arguments.get("content");

        if (recipient == null || content == null) {
            return errorResponse("INVALID_ARGUMENTS", "缺少必要参数: recipient 或 content");
        }

        // 语义对齐：根据昵称或标签找到联系人
        List<Friend> candidates;
        try {
            // 如果是大模型直接返回了数字 ID
            Long directId = Long.valueOf(recipient);
            candidates = new ArrayList<>();
            Friend f = new Friend();
            f.setFriendId(directId);
            f.setFriendNickname("用户" + directId);
            candidates.add(f);
        } catch (NumberFormatException e) {
            candidates = friendService.resolveRecipient(userId, recipient);
            // 语义增强：如果好友列表没找到，尝试在全局用户表里找（作为补充）
            if (candidates.isEmpty()) {
                org.linxin.server.module.user.entity.User globalUser = userMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.linxin.server.module.user.entity.User>()
                                .eq(org.linxin.server.module.user.entity.User::getUsername, recipient));
                if (globalUser != null) {
                    Friend virtualFriend = new Friend();
                    virtualFriend.setFriendId(globalUser.getId());
                    virtualFriend.setFriendNickname(globalUser.getNickname());
                    candidates.add(virtualFriend);
                }
            }
        }

        if (candidates.isEmpty()) {
            return errorResponse("NOT_FOUND", "未找到匹配联系人: " + recipient);
        }

        if (candidates.size() > 1) {
            List<Map<String, String>> choices = new ArrayList<>();
            for (Friend f : candidates) {
                choices.add(Map.of(
                        "hint", f.getFriendNickname() + (f.getTags() != null ? " (" + f.getTags() + ")" : ""),
                        "id", f.getFriendId().toString()));
            }
            Map<String, Object> res = errorResponse("AMBIGUOUS_RECIPIENT", "发现多个匹配项，请确认：");
            res.put("choices", choices);
            return res;
        }

        // 执行发送
        Long targetId = candidates.get(0).getFriendId();
        Message msg = messageService.sendAgentMessage(userId, targetId, content, "内置AI小助手");

        Map<String, Object> success = new HashMap<>();
        success.put("status", "SUCCESS");
        success.put("data", Map.of(
                "target", candidates.get(0).getFriendNickname(),
                "sequenceId", msg.getSequenceId()));
        return success;
    }

    private Map<String, Object> errorResponse(String status, String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", status);
        res.put("message", message);
        return res;
    }
}
