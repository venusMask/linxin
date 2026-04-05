package org.linxin.server.module.group.service;

import java.util.List;
import org.linxin.server.module.group.model.request.AddGroupMembersRequest;
import org.linxin.server.module.group.model.request.CreateGroupRequest;
import org.linxin.server.module.group.vo.GroupMemberVO;
import org.linxin.server.module.group.vo.GroupVO;

public interface IGroupService {

    GroupVO createGroup(Long ownerId, CreateGroupRequest request);

    GroupVO getGroupInfo(Long groupId);

    List<GroupMemberVO> getGroupMembers(Long groupId);

    GroupVO addMembers(Long groupId, Long operatorId, AddGroupMembersRequest request);

    void removeMember(Long groupId, Long userId, Long operatorId);

    void leaveGroup(Long groupId, Long userId);

    void dissolveGroup(Long groupId, Long ownerId);

    void updateGroupAnnouncement(Long groupId, Long operatorId, String announcement);

    boolean isGroupMember(Long groupId, Long userId);

    List<GroupVO> getUserGroups(Long userId);
}
