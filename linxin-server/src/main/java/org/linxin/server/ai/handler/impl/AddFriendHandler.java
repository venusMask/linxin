package org.linxin.server.ai.handler.impl;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.handler.AIToolHandler;
import org.linxin.server.module.contact.model.request.FriendApplyRequest;
import org.linxin.server.module.contact.service.IFriendService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddFriendHandler implements AIToolHandler {

    private final IFriendService friendService;
    private final org.linxin.server.module.user.mapper.UserMapper userMapper;

    @Override
    public String getToolName() {
        return "add_friend";
    }

    @Override
    public Map<String, Object> execute(Long userId, Map<String, Object> arguments) {
        Object targetIdVal = arguments.get("userId");
        if (targetIdVal == null) {
            targetIdVal = arguments.get("friendId"); // 兼容字段
        }
        String remark = (String) arguments.get("remark");

        if (targetIdVal == null) {
            return errorResponse("INVALID_ARGUMENTS", "缺少必要参数: userId");
        }

        Long targetId = null;
        try {
            targetId = Long.valueOf(targetIdVal.toString());
        } catch (NumberFormatException e) {
            // 尝试作为用户名查找
            org.linxin.server.module.user.entity.User user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.linxin.server.module.user.entity.User>()
                            .eq(org.linxin.server.module.user.entity.User::getUsername, targetIdVal.toString()));
            if (user != null) {
                targetId = user.getId();
            }
        }

        if (targetId == null) {
            return errorResponse("NOT_FOUND", "找不到要添加的用户: " + targetIdVal);
        }

        try {
            FriendApplyRequest request = new FriendApplyRequest();
            request.setFriendId(targetId);
            request.setRemark(remark != null ? remark : "AI 助手代为申请");

            friendService.applyAddFriend(userId, request);

            Map<String, Object> success = new HashMap<>();
            success.put("status", "SUCCESS");
            success.put("data", Map.of("targetId", targetId));
            return success;
        } catch (Exception e) {
            log.error("Failed to add friend via AI", e);
            return errorResponse("EXECUTION_FAILED", "申请添加好友失败: " + e.getMessage());
        }
    }

    private Map<String, Object> errorResponse(String status, String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", status);
        res.put("message", message);
        return res;
    }
}
