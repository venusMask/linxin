package org.linxin.server.business.service;

import org.linxin.server.business.model.request.AddGroupMembersRequest;
import org.linxin.server.business.model.request.CreateGroupRequest;
import org.linxin.server.business.vo.GroupMemberVO;
import org.linxin.server.business.vo.GroupVO;

import java.util.List;

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
