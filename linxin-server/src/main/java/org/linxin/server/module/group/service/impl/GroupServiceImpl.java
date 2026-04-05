package org.linxin.server.module.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.linxin.server.common.exception.BusinessException;
import org.linxin.server.module.chat.entity.Conversation;
import org.linxin.server.module.chat.mapper.ConversationMapper;
import org.linxin.server.module.group.entity.Group;
import org.linxin.server.module.group.entity.GroupMember;
import org.linxin.server.module.group.mapper.GroupMapper;
import org.linxin.server.module.group.mapper.GroupMemberMapper;
import org.linxin.server.module.group.model.request.AddGroupMembersRequest;
import org.linxin.server.module.group.model.request.CreateGroupRequest;
import org.linxin.server.module.group.service.IGroupService;
import org.linxin.server.module.group.vo.GroupMemberVO;
import org.linxin.server.module.group.vo.GroupVO;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements IGroupService {

    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;

    private static final int ROLE_OWNER = 2;
    private static final int ROLE_ADMIN = 1;
    private static final int ROLE_MEMBER = 0;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO createGroup(Long ownerId, CreateGroupRequest request) {
        User owner = userMapper.selectById(ownerId);
        if (owner == null) {
            throw new BusinessException("用户不存在");
        }

        Group group = new Group();
        group.setName(request.getName());
        group.setAvatar(request.getAvatar());
        group.setOwnerId(ownerId);
        group.setMemberLimit(500);
        group.setMemberCount(0);
        group.setStatus(0);
        group.setCreateTime(LocalDateTime.now());
        groupMapper.insert(group);

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setNickname(owner.getNickname());
        ownerMember.setRole(ROLE_OWNER);
        ownerMember.setJoinTime(LocalDateTime.now());
        ownerMember.setMuteStatus(0);
        ownerMember.setCreateTime(LocalDateTime.now());
        groupMemberMapper.insert(ownerMember);

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            addMembersToGroup(group.getId(), request.getMemberIds(), ownerId);
        }

        return getGroupInfo(group.getId());
    }

    @Override
    public GroupVO getGroupInfo(Long groupId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException("群组不存在");
        }

        GroupVO vo = toGroupVO(group);

        User owner = userMapper.selectById(group.getOwnerId());
        if (owner != null) {
            vo.setOwnerNickname(owner.getNickname());
        }

        return vo;
    }

    @Override
    public List<GroupMemberVO> getGroupMembers(Long groupId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getDeleted, 0)
                .orderByAsc(GroupMember::getRole);

        List<GroupMember> members = groupMemberMapper.selectList(wrapper);
        Set<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream().map(member -> {
            GroupMemberVO vo = toGroupMemberVO(member);
            User user = userMap.get(member.getUserId());
            if (user != null) {
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO addMembers(Long groupId, Long operatorId, AddGroupMembersRequest request) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException("群组不存在");
        }

        GroupMember operator = getGroupMember(groupId, operatorId);
        if (operator == null || operator.getRole() == ROLE_MEMBER) {
            throw new BusinessException("无权限操作");
        }

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            addMembersToGroup(groupId, request.getMemberIds(), operatorId);
        }

        return getGroupInfo(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException("群组不存在");
        }

        GroupMember operator = getGroupMember(groupId, operatorId);
        if (operator == null || operator.getRole() == ROLE_MEMBER) {
            throw new BusinessException("无权限操作");
        }

        GroupMember targetMember = getGroupMember(groupId, userId);
        if (targetMember == null) {
            throw new BusinessException("该成员不在群组中");
        }

        if (targetMember.getRole() == ROLE_OWNER) {
            throw new BusinessException("群主不能被移除");
        }

        targetMember.setDeleted(1);
        groupMemberMapper.updateById(targetMember);

        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        groupMapper.updateById(group);

        LambdaQueryWrapper<Conversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(Conversation::getUserId, userId)
                .eq(Conversation::getGroupId, groupId)
                .eq(Conversation::getDeleted, 0);
        List<Conversation> conversations = conversationMapper.selectList(convWrapper);
        for (Conversation conv : conversations) {
            conv.setDeleted(1);
            conversationMapper.updateById(conv);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException("群组不存在");
        }

        GroupMember member = getGroupMember(groupId, userId);
        if (member == null) {
            throw new BusinessException("你不在群组中");
        }

        if (member.getRole() == ROLE_OWNER) {
            throw new BusinessException("群主不能退群，请先转让群主或解散群");
        }

        member.setDeleted(1);
        groupMemberMapper.updateById(member);

        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        groupMapper.updateById(group);

        LambdaQueryWrapper<Conversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(Conversation::getUserId, userId)
                .eq(Conversation::getGroupId, groupId)
                .eq(Conversation::getDeleted, 0);
        List<Conversation> conversations = conversationMapper.selectList(convWrapper);
        for (Conversation conv : conversations) {
            conv.setDeleted(1);
            conversationMapper.updateById(conv);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long groupId, Long ownerId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException("群组不存在");
        }

        if (!group.getOwnerId().equals(ownerId)) {
            throw new BusinessException("只有群主才能解散群");
        }

        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, groupId);
        List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
        for (GroupMember member : members) {
            member.setDeleted(1);
            groupMemberMapper.updateById(member);
        }

        LambdaQueryWrapper<Conversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(Conversation::getGroupId, groupId);
        List<Conversation> conversations = conversationMapper.selectList(convWrapper);
        for (Conversation conv : conversations) {
            conv.setDeleted(1);
            conversationMapper.updateById(conv);
        }

        group.setDeleted(1);
        groupMapper.updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroupAnnouncement(Long groupId, Long operatorId, String announcement) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException("群组不存在");
        }

        GroupMember operator = getGroupMember(groupId, operatorId);
        if (operator == null || operator.getRole() == ROLE_MEMBER) {
            throw new BusinessException("无权限操作");
        }

        group.setAnnouncement(announcement);
        groupMapper.updateById(group);
    }

    @Override
    public boolean isGroupMember(Long groupId, Long userId) {
        return getGroupMember(groupId, userId) != null;
    }

    @Override
    public List<GroupVO> getUserGroups(Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getDeleted, 0);

        List<GroupMember> memberships = groupMemberMapper.selectList(wrapper);
        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> groupIds = memberships.stream().map(GroupMember::getGroupId).collect(Collectors.toSet());
        List<Group> groups = groupMapper.selectBatchIds(groupIds).stream()
                .filter(g -> g.getDeleted() == 0)
                .collect(Collectors.toList());

        return groups.stream().map(this::toGroupVO).collect(Collectors.toList());
    }

    private void addMembersToGroup(Long groupId, List<Long> userIds, Long operatorId) {
        Group group = groupMapper.selectById(groupId);

        for (Long userId : userIds) {
            if (getGroupMember(groupId, userId) != null) {
                continue;
            }

            User user = userMapper.selectById(userId);
            if (user == null) {
                continue;
            }

            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserId(userId);
            member.setNickname(user.getNickname());
            member.setRole(ROLE_MEMBER);
            member.setJoinTime(LocalDateTime.now());
            member.setMuteStatus(0);
            member.setCreateTime(LocalDateTime.now());
            groupMemberMapper.insert(member);

            createGroupConversation(userId, groupId, group.getName());
        }

        group.setMemberCount(groupMemberMapper.selectCount(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getDeleted, 0))
                .intValue());
        groupMapper.updateById(group);
    }

    private void createGroupConversation(Long userId, Long groupId, String groupName) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
                .eq(Conversation::getGroupId, groupId)
                .eq(Conversation::getDeleted, 0);
        Conversation existing = conversationMapper.selectOne(wrapper);
        if (existing != null) {
            return;
        }

        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setPeerId(0L);
        conversation.setPeerNickname(groupName);
        conversation.setPeerAvatar(null);
        conversation.setType(1);
        conversation.setGroupId(groupId);
        conversation.setUnreadCount(0);
        conversation.setTopStatus(0);
        conversation.setMuteStatus(0);
        conversation.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
    }

    private GroupMember getGroupMember(Long groupId, Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getDeleted, 0);
        return groupMemberMapper.selectOne(wrapper);
    }

    private GroupVO toGroupVO(Group group) {
        GroupVO vo = new GroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setAvatar(group.getAvatar());
        vo.setOwnerId(group.getOwnerId());
        vo.setAnnouncement(group.getAnnouncement());
        vo.setMemberLimit(group.getMemberLimit());
        vo.setMemberCount(group.getMemberCount());
        vo.setStatus(group.getStatus());
        vo.setCreateTime(group.getCreateTime());
        return vo;
    }

    private GroupMemberVO toGroupMemberVO(GroupMember member) {
        GroupMemberVO vo = new GroupMemberVO();
        vo.setId(member.getId());
        vo.setGroupId(member.getGroupId());
        vo.setUserId(member.getUserId());
        vo.setNickname(member.getNickname());
        vo.setRole(member.getRole());
        vo.setJoinTime(member.getJoinTime());
        vo.setMuteStatus(member.getMuteStatus());
        return vo;
    }
}
