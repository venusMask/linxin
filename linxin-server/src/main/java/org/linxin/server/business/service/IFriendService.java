package org.linxin.server.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import org.linxin.server.business.entity.Friend;
import org.linxin.server.business.model.request.FriendApplyRequest;
import org.linxin.server.business.model.request.FriendHandleRequest;
import org.linxin.server.business.model.request.FriendUpdateRequest;
import org.linxin.server.business.vo.FriendApplyVO;
import org.linxin.server.business.vo.FriendVO;

public interface IFriendService extends IService<Friend> {
    IPage<FriendVO> getFriendList(Long userId, String keyword, Integer pageNum, Integer pageSize);

    void applyAddFriend(Long userId, FriendApplyRequest request);

    List<FriendApplyVO> getReceivedApplyList(Long userId);

    List<FriendApplyVO> getSentApplyList(Long userId);

    void handleFriendApply(Long userId, FriendHandleRequest request);

    void deleteFriend(Long userId, Long friendId);

    void updateFriend(Long userId, FriendUpdateRequest request);

    boolean isFriend(Long userId, Long friendId);

    List<Friend> resolveRecipient(Long userId, String recipientName);
}
