package org.venus.lin.xin.mgr.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.venus.lin.xin.mgr.business.entity.Friend;
import org.venus.lin.xin.mgr.business.entity.FriendApply;
import org.venus.lin.xin.mgr.business.model.request.FriendApplyRequest;
import org.venus.lin.xin.mgr.business.model.request.FriendHandleRequest;
import org.venus.lin.xin.mgr.business.model.request.FriendUpdateRequest;
import org.venus.lin.xin.mgr.business.vo.FriendApplyVO;
import org.venus.lin.xin.mgr.business.vo.FriendVO;

import java.util.List;

public interface IFriendService {

    IPage<FriendVO> getFriendList(String username, Integer pageNum, Integer pageSize);

    FriendApply applyAddFriend(Long fromUserId, FriendApplyRequest request);

    List<FriendApplyVO> getReceivedApplyList(Long userId);

    List<FriendApplyVO> getSentApplyList(Long userId);

    void handleFriendApply(Long toUserId, FriendHandleRequest request);

    void deleteFriend(Long userId, Long friendId);

    void updateFriend(Long userId, FriendUpdateRequest request);

    boolean isFriend(Long userId, Long friendId);
}
