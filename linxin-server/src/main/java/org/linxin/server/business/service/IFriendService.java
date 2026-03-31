package org.linxin.server.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.linxin.server.business.entity.FriendApply;
import org.linxin.server.business.model.request.FriendApplyRequest;
import org.linxin.server.business.model.request.FriendHandleRequest;
import org.linxin.server.business.model.request.FriendUpdateRequest;
import org.linxin.server.business.vo.FriendApplyVO;
import org.linxin.server.business.vo.FriendVO;

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
