package org.linxin.server.module.group.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.linxin.server.common.result.Result;
import org.linxin.server.module.group.model.request.AddGroupMembersRequest;
import org.linxin.server.module.group.model.request.CreateGroupRequest;
import org.linxin.server.module.group.model.request.UpdateAnnouncementRequest;
import org.linxin.server.module.group.service.IGroupService;
import org.linxin.server.module.group.vo.GroupMemberVO;
import org.linxin.server.module.group.vo.GroupVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
@Tag(name = "群组模块", description = "群组相关操作接口")
public class GroupController {

    private final IGroupService groupService;

    @PostMapping("/create")
    @Operation(summary = "创建群组")
    public Result<GroupVO> createGroup(
            @RequestAttribute("userId") Long userId,
            @RequestBody CreateGroupRequest request) {
        GroupVO group = groupService.createGroup(userId, request);
        return Result.success(group);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "获取群组信息")
    public Result<GroupVO> getGroupInfo(@PathVariable Long groupId) {
        GroupVO group = groupService.getGroupInfo(groupId);
        return Result.success(group);
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "获取群组成员列表")
    public Result<List<GroupMemberVO>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMemberVO> members = groupService.getGroupMembers(groupId);
        return Result.success(members);
    }

    @PostMapping("/{groupId}/members/add")
    @Operation(summary = "添加群成员")
    public Result<GroupVO> addMembers(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId,
            @RequestBody AddGroupMembersRequest request) {
        GroupVO group = groupService.addMembers(groupId, userId, request);
        return Result.success(group);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "移除群成员")
    public Result<String> removeMember(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId,
            @PathVariable Long memberId) {
        groupService.removeMember(groupId, memberId, userId);
        return Result.success("成员已移除");
    }

    @PostMapping("/{groupId}/leave")
    @Operation(summary = "退出群组")
    public Result<String> leaveGroup(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId) {
        groupService.leaveGroup(groupId, userId);
        return Result.success("已退出群组");
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "解散群组")
    public Result<String> dissolveGroup(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId) {
        groupService.dissolveGroup(groupId, userId);
        return Result.success("群组已解散");
    }

    @PutMapping("/{groupId}/announcement")
    @Operation(summary = "更新群公告")
    public Result<String> updateAnnouncement(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long groupId,
            @RequestBody UpdateAnnouncementRequest request) {
        groupService.updateGroupAnnouncement(groupId, userId, request.getAnnouncement());
        return Result.success("群公告已更新");
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的群组列表")
    public Result<List<GroupVO>> getMyGroups(@RequestAttribute("userId") Long userId) {
        List<GroupVO> groups = groupService.getUserGroups(userId);
        return Result.success(groups);
    }
}
