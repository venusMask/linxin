package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.linxin.server.business.converter.FriendConverter;
import org.linxin.server.business.entity.Friend;
import org.linxin.server.business.entity.FriendApply;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.FriendApplyMapper;
import org.linxin.server.business.mapper.FriendMapper;
import org.linxin.server.business.mapper.UserMapper;
import org.linxin.server.business.model.request.FriendApplyRequest;
import org.linxin.server.business.model.request.FriendHandleRequest;
import org.linxin.server.business.model.request.FriendUpdateRequest;
import org.linxin.server.business.service.IFriendService;
import org.linxin.server.business.vo.FriendApplyVO;
import org.linxin.server.business.vo.FriendVO;
import org.linxin.server.common.constant.ApplyStatus;
import org.linxin.server.common.constant.FriendStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements IFriendService {

    private final FriendMapper friendMapper;
    private final FriendApplyMapper friendApplyMapper;
    private final UserMapper userMapper;
    private final FriendConverter friendConverter;

    @Override
    public IPage<FriendVO> getFriendList(String username, Integer pageNum, Integer pageSize) {
        Page<FriendVO> page = new Page<>(pageNum, pageSize);
        return friendMapper.selectFriendPage(page, username);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FriendApply applyAddFriend(Long fromUserId, FriendApplyRequest request) {
        if (fromUserId.equals(request.getToUserId())) {
            throw new RuntimeException("不能添加自己为好友");
        }
        
        User toUser = userMapper.selectById(request.getToUserId());
        if (toUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        int count = friendMapper.isFriend(fromUserId, request.getToUserId());
        if (count > 0) {
            throw new RuntimeException("你们已经是好友了");
        }
        
        LambdaQueryWrapper<FriendApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendApply::getFromUserId, fromUserId)
                .eq(FriendApply::getToUserId, request.getToUserId())
                .eq(FriendApply::getStatus, ApplyStatus.PENDING)
                .eq(FriendApply::getDeleted, 0);
        if (friendApplyMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("已发送过好友申请，请等待对方处理");
        }
        
        User fromUser = userMapper.selectById(fromUserId);
        FriendApply apply = new FriendApply();
        apply.setFromUserId(fromUserId);
        apply.setToUserId(request.getToUserId());
        apply.setFromNickname(fromUser.getNickname());
        apply.setFromAvatar(fromUser.getAvatar());
        apply.setRemark(request.getRemark());
        apply.setStatus(ApplyStatus.PENDING);
        apply.setReadStatus(0);
        friendApplyMapper.insert(apply);
        return apply;
    }

    @Override
    public List<FriendApplyVO> getReceivedApplyList(Long userId) {
        LambdaQueryWrapper<FriendApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendApply::getToUserId, userId)
                .eq(FriendApply::getDeleted, 0)
                .orderByDesc(FriendApply::getCreateTime);
        return friendApplyMapper.selectList(wrapper).stream()
                .map(friendConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendApplyVO> getSentApplyList(Long userId) {
        LambdaQueryWrapper<FriendApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendApply::getFromUserId, userId)
                .eq(FriendApply::getDeleted, 0)
                .orderByDesc(FriendApply::getCreateTime);
        return friendApplyMapper.selectList(wrapper).stream()
                .map(friendConverter::toVO)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handleFriendApply(Long toUserId, FriendHandleRequest request) {
        FriendApply apply = friendApplyMapper.selectById(request.getApplyId());
        if (apply == null || !apply.getToUserId().equals(toUserId)) {
            throw new RuntimeException("好友申请不存在");
        }
        
        if (apply.getStatus() != ApplyStatus.PENDING) {
            throw new RuntimeException("该申请已处理");
        }
        
        apply.setStatus(request.getStatus());
        apply.setHandleTime(LocalDateTime.now());
        friendApplyMapper.updateById(apply);
        
        if (request.getStatus() == ApplyStatus.ACCEPTED) {
            User fromUser = userMapper.selectById(apply.getFromUserId());
            User toUser = userMapper.selectById(apply.getToUserId());
            
            Friend friend1 = new Friend();
            friend1.setUserId(apply.getFromUserId());
            friend1.setFriendId(apply.getToUserId());
            friend1.setFriendNickname(toUser.getNickname());
            friend1.setFriendGroup("默认分组");
            friend1.setStatus(FriendStatus.FRIENDED);
            friendMapper.insert(friend1);
            
            Friend friend2 = new Friend();
            friend2.setUserId(apply.getToUserId());
            friend2.setFriendId(apply.getFromUserId());
            friend2.setFriendNickname(fromUser.getNickname());
            friend2.setFriendGroup("默认分组");
            friend2.setStatus(FriendStatus.FRIENDED);
            friendMapper.insert(friend2);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId);
        Friend friend1 = friendMapper.selectOne(wrapper);
        if (friend1 != null) {
            friend1.setDeleted(1);
            friendMapper.updateById(friend1);
        }
        
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId);
        Friend friend2 = friendMapper.selectOne(wrapper);
        if (friend2 != null) {
            friend2.setDeleted(1);
            friendMapper.updateById(friend2);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateFriend(Long userId, FriendUpdateRequest request) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, request.getFriendId());
        Friend friend = friendMapper.selectOne(wrapper);
        if (friend != null) {
            if (request.getFriendNickname() != null) {
                friend.setFriendNickname(request.getFriendNickname());
            }
            if (request.getFriendGroup() != null) {
                friend.setFriendGroup(request.getFriendGroup());
            }
            friendMapper.updateById(friend);
        }
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return friendMapper.isFriend(userId, friendId) > 0;
    }
}
