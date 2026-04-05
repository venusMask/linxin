package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements IFriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final FriendApplyMapper friendApplyMapper;

    @Override
    public IPage<FriendVO> getFriendList(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        Page<Friend> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Friend::getFriendNickname, keyword).or().like(Friend::getTags, keyword));
        }
        IPage<Friend> friendPage = friendMapper.selectPage(page, wrapper);

        // 如果是第一页，且没有搜索关键词，或者关键词匹配 AI，则手动加入系统 AI
        List<FriendVO> records = friendPage.getRecords().stream().map(f -> {
            FriendVO vo = new FriendVO();
            vo.setId(f.getId());
            vo.setUserId(f.getUserId());
            vo.setFriendId(f.getFriendId());
            vo.setFriendNickname(f.getFriendNickname());
            vo.setFriendGroup(f.getFriendGroup());
            vo.setTags(f.getTags());
            vo.setCreateTime(f.getCreateTime());
            User user = userMapper.selectById(f.getFriendId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
                vo.setUserStatus(user.getStatus());
                vo.setSignature(user.getSignature());
                vo.setUserType(user.getUserType());
                if (vo.getFriendNickname() == null)
                    vo.setFriendNickname(user.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());

        if (pageNum == 1 && (keyword == null || keyword.isEmpty() || "AI".contains(keyword.toUpperCase())
                || "助手".contains(keyword))) {
            User aiUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserType, 1).last("LIMIT 1"));
            if (aiUser != null) {
                // 检查是否已经在列表中（万一用户真的手动加了它）
                boolean exists = records.stream().anyMatch(f -> aiUser.getId().equals(f.getFriendId()));
                if (!exists) {
                    FriendVO aiVo = new FriendVO();
                    aiVo.setFriendId(aiUser.getId());
                    aiVo.setFriendNickname(aiUser.getNickname());
                    aiVo.setUsername(aiUser.getUsername());
                    aiVo.setAvatar(aiUser.getAvatar());
                    aiVo.setUserStatus(aiUser.getStatus());
                    aiVo.setSignature("我是您的AI智能助手");
                    aiVo.setUserType(1);
                    records.add(0, aiVo);
                }
            }
        }

        IPage<FriendVO> voPage = new Page<>(pageNum, pageSize,
                friendPage.getTotal() + (records.size() > friendPage.getRecords().size() ? 1 : 0));
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAddFriend(Long userId, FriendApplyRequest request) {
        User fromUser = userMapper.selectById(userId);
        FriendApply apply = new FriendApply();
        apply.setFromUserId(userId);
        apply.setToUserId(request.getFriendId());
        apply.setRemark(request.getRemark());
        apply.setFromNickname(fromUser.getNickname());
        apply.setFromAvatar(fromUser.getAvatar());
        apply.setStatus(0);
        apply.setReadStatus(0);
        friendApplyMapper.insert(apply);
    }

    @Override
    public List<FriendApplyVO> getReceivedApplyList(Long userId) {
        LambdaQueryWrapper<FriendApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendApply::getToUserId, userId).orderByDesc(FriendApply::getCreateTime);
        return friendApplyMapper.selectList(wrapper).stream().map(this::toApplyVO).collect(Collectors.toList());
    }

    @Override
    public List<FriendApplyVO> getSentApplyList(Long userId) {
        LambdaQueryWrapper<FriendApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendApply::getFromUserId, userId).orderByDesc(FriendApply::getCreateTime);
        return friendApplyMapper.selectList(wrapper).stream().map(this::toApplyVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFriendApply(Long userId, FriendHandleRequest request) {
        FriendApply apply = friendApplyMapper.selectById(request.getApplyId());
        if (apply == null || !apply.getToUserId().equals(userId))
            return;

        apply.setStatus(request.getStatus());
        apply.setHandleTime(LocalDateTime.now());
        friendApplyMapper.updateById(apply);

        if (request.getStatus() == 1) { // 同意
            addFriendPair(apply.getFromUserId(), apply.getToUserId());
        }
    }

    private void addFriendPair(Long user1, Long user2) {
        if (!isFriend(user1, user2)) {
            User u1 = userMapper.selectById(user1);
            User u2 = userMapper.selectById(user2);

            Friend f1 = new Friend();
            f1.setUserId(user1);
            f1.setFriendId(user2);
            f1.setFriendNickname(u2.getNickname());
            f1.setStatus(1);
            friendMapper.insert(f1);

            Friend f2 = new Friend();
            f2.setUserId(user2);
            f2.setFriendId(user1);
            f2.setFriendNickname(u1.getNickname());
            f2.setStatus(1);
            friendMapper.insert(f2);
        }
    }

    private FriendApplyVO toApplyVO(FriendApply a) {
        FriendApplyVO vo = new FriendApplyVO();
        vo.setId(a.getId());
        vo.setFromUserId(a.getFromUserId());
        vo.setToUserId(a.getToUserId());
        vo.setRemark(a.getRemark());
        vo.setStatus(a.getStatus());
        vo.setFromNickname(a.getFromNickname());
        vo.setFromAvatar(a.getFromAvatar());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFriend(Long userId, FriendUpdateRequest request) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, request.getFriendId());
        Friend friend = friendMapper.selectOne(wrapper);
        if (friend != null) {
            if (request.getFriendNickname() != null)
                friend.setFriendNickname(request.getFriendNickname());
            if (request.getFriendGroup() != null)
                friend.setFriendGroup(request.getFriendGroup());
            if (request.getTags() != null)
                friend.setTags(request.getTags());
            friendMapper.updateById(friend);
        }
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        friendMapper.delete(
                new LambdaQueryWrapper<Friend>().eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId));
        friendMapper.delete(
                new LambdaQueryWrapper<Friend>().eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId));
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>().eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)) > 0;
    }

    @Override
    public List<Friend> resolveRecipient(Long userId, String recipientName) {
        User targetUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, recipientName));
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .and(w -> {
                    w.like(Friend::getFriendNickname, recipientName).or().like(Friend::getTags, recipientName);
                    if (targetUser != null)
                        w.or().eq(Friend::getFriendId, targetUser.getId());
                    try {
                        Long id = Long.parseLong(recipientName);
                        w.or().eq(Friend::getFriendId, id);
                    } catch (NumberFormatException ignored) {
                    }
                });
        return friendMapper.selectList(wrapper);
    }
}
