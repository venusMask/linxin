package org.linxin.server.module.contact.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import org.linxin.server.module.contact.entity.Friend;
import org.linxin.server.module.contact.model.request.FriendApplyRequest;
import org.linxin.server.module.contact.model.request.FriendHandleRequest;
import org.linxin.server.module.contact.model.request.FriendUpdateRequest;
import org.linxin.server.module.contact.vo.FriendApplyVO;
import org.linxin.server.module.contact.vo.FriendVO;

public interface IFriendService extends IService<Friend> {
    IPage<FriendVO> getFriendList(Long userId, String keyword, Integer pageNum, Integer pageSize);

    void applyAddFriend(Long userId, FriendApplyRequest request);

    List<FriendApplyVO> getReceivedApplyList(Long userId);

    List<FriendApplyVO> getSentApplyList(Long userId);

    void handleFriendApply(Long userId, FriendHandleRequest request);

    void deleteFriend(Long userId, Long friendId);

    void updateFriend(Long userId, FriendUpdateRequest request);

    boolean isFriend(Long userId, Long friendId);

    List<FriendVO> syncFriends(Long userId, Long lastSequenceId);

    List<Friend> resolveRecipient(Long userId, String recipientName);
}
