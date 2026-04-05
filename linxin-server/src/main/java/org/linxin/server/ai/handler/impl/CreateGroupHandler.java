package org.linxin.server.ai.handler.impl;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.handler.AIToolHandler;
import org.linxin.server.business.model.request.CreateGroupRequest;
import org.linxin.server.business.service.IGroupService;
import org.linxin.server.business.vo.GroupVO;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateGroupHandler implements AIToolHandler {

    private final IGroupService groupService;
    private final org.linxin.server.business.mapper.UserMapper userMapper;

    @Override
    public String getToolName() {
        return "create_group";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Long userId, Map<String, Object> arguments) {
        String groupName = (String) arguments.get("groupName");
        Object memberIdsObj = arguments.get("memberIds");

        if (groupName == null || memberIdsObj == null) {
            return errorResponse("INVALID_ARGUMENTS", "缺少必要参数: groupName 或 memberIds");
        }

        List<Long> memberIds = new ArrayList<>();
        // 默认把自己加入
        memberIds.add(userId);

        if (memberIdsObj instanceof List) {
            for (Object item : (List<?>) memberIdsObj) {
                String val = item.toString();
                if ("myself".equalsIgnoreCase(val) || "me".equalsIgnoreCase(val)) {
                    continue; // 已经加过了
                }
                try {
                    memberIds.add(Long.valueOf(val));
                } catch (NumberFormatException e) {
                    // 语义解析：如果不是数字，尝试作为用户名查找
                    org.linxin.server.business.entity.User user = userMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.linxin.server.business.entity.User>()
                                    .eq(org.linxin.server.business.entity.User::getUsername, val));
                    if (user != null) {
                        memberIds.add(user.getId());
                    }
                }
            }
        }

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(groupName);
        request.setMemberIds(memberIds);

        try {
            GroupVO group = groupService.createGroup(userId, request);
            Map<String, Object> success = new HashMap<>();
            success.put("status", "SUCCESS");
            success.put("data", Map.of(
                    "groupId", group.getId(),
                    "groupName", group.getName()));
            return success;
        } catch (Exception e) {
            log.error("Failed to create group via AI", e);
            return errorResponse("EXECUTION_FAILED", "创建群聊失败: " + e.getMessage());
        }
    }

    private Map<String, Object> errorResponse(String status, String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", status);
        res.put("message", message);
        return res;
    }
}
