package org.linxin.server.module.contact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.linxin.server.module.contact.entity.Friend;
import org.linxin.server.module.contact.entity.FriendApply;
import org.linxin.server.module.contact.mapper.FriendApplyMapper;
import org.linxin.server.module.contact.mapper.FriendMapper;
import org.linxin.server.module.contact.model.request.FriendApplyRequest;
import org.linxin.server.module.contact.model.request.FriendHandleRequest;
import org.linxin.server.module.contact.model.request.FriendUpdateRequest;
import org.linxin.server.module.contact.service.IFriendService;
import org.linxin.server.module.contact.vo.FriendApplyVO;
import org.linxin.server.module.contact.vo.FriendVO;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements IFriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final FriendApplyMapper friendApplyMapper;
    private final org.linxin.server.common.util.SnowflakeIdWorker snowflakeIdWorker;
    private final org.linxin.server.websocket.WebSocketHandler webSocketHandler;

    public FriendServiceImpl(
            FriendMapper friendMapper,
            UserMapper userMapper,
            FriendApplyMapper friendApplyMapper,
            org.linxin.server.common.util.SnowflakeIdWorker snowflakeIdWorker,
            @org.springframework.context.annotation.Lazy org.linxin.server.websocket.WebSocketHandler webSocketHandler) {
        this.friendMapper = friendMapper;
        this.userMapper = userMapper;
        this.friendApplyMapper = friendApplyMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.webSocketHandler = webSocketHandler;
    }

    private Long getNextSequenceId(Long userId) {
        return snowflakeIdWorker.nextId();
    }

    @Override
    public IPage<FriendVO> getFriendList(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        Page<Friend> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Friend::getFriendNickname, keyword).or().like(Friend::getTags, keyword));
        }
        IPage<Friend> friendPage = friendMapper.selectPage(page, wrapper);

        return friendPage.convert(f -> {
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
                vo.setNickname(user.getNickname());
                if (vo.getFriendNickname() == null)
                    vo.setFriendNickname(user.getNickname());
            }
            return vo;
        });
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAddFriend(Long userId, FriendApplyRequest request) {
        if (userId.equals(request.getFriendId())) {
            throw new org.linxin.server.common.exception.BusinessException("不能添加自己为好友");
        }

        // 1. 检查是否已经是好友
        if (isFriend(userId, request.getFriendId())) {
            throw new org.linxin.server.common.exception.BusinessException("对方已经是您的好友");
        }

        // 2. 检查是否已经发送过申请且对方尚未处理 (status = 0)
        LambdaQueryWrapper<FriendApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendApply::getFromUserId, userId)
                .eq(FriendApply::getToUserId, request.getFriendId())
                .eq(FriendApply::getStatus, 0);
        if (friendApplyMapper.selectCount(wrapper) > 0) {
            throw new org.linxin.server.common.exception.BusinessException("申请已发送，请等待对方处理");
        }

        User fromUser = userMapper.selectById(userId);
        FriendApply apply = new FriendApply();
        // ... (保持后续插入逻辑)

        apply.setFromUserId(userId);
        apply.setToUserId(request.getFriendId());
        apply.setRemark(request.getRemark());
        apply.setFromNickname(fromUser.getNickname());
        apply.setFromAvatar(fromUser.getAvatar());
        apply.setStatus(0);
        apply.setReadStatus(0);
        friendApplyMapper.insert(apply);

        // 通知接收方有新好友申请
        java.util.Map<String, Object> dataApply = new java.util.HashMap<>();
        dataApply.put("fromUserId", userId);
        webSocketHandler.sendMessageToUser(request.getFriendId(),
                new org.linxin.server.websocket.WebSocketMessage("friend_apply", dataApply));
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

            // 推送通知给双方，触发增量同步
            java.util.Map<String, Object> data1 = new java.util.HashMap<>();
            data1.put("status", 1);
            data1.put("friendId", apply.getToUserId());
            webSocketHandler.sendMessageToUser(apply.getFromUserId(),
                    new org.linxin.server.websocket.WebSocketMessage("friend_handle", data1));

            java.util.Map<String, Object> data2 = new java.util.HashMap<>();
            data2.put("status", 1);
            data2.put("friendId", apply.getFromUserId());
            webSocketHandler.sendMessageToUser(apply.getToUserId(),
                    new org.linxin.server.websocket.WebSocketMessage("friend_handle", data2));
        } else {
            // 拒绝申请也通知发起人
            java.util.Map<String, Object> data3 = new java.util.HashMap<>();
            data3.put("status", request.getStatus());
            data3.put("friendId", apply.getToUserId());
            webSocketHandler.sendMessageToUser(apply.getFromUserId(),
                    new org.linxin.server.websocket.WebSocketMessage("friend_handle", data3));
        }
    }

    private void addFriendPair(Long user1, Long user2) {
        if (!isFriend(user1, user2)) {
            User u1 = userMapper.selectById(user1);
            User u2 = userMapper.selectById(user2);

            insertOrRestoreFriend(user1, user2, u2.getNickname());
            insertOrRestoreFriend(user2, user1, u1.getNickname());
        }
    }

    private void insertOrRestoreFriend(Long userId, Long friendId, String friendNickname) {
        // 使用自定义 Mapper 方法查询，包含逻辑删除的记录
        Friend friend = friendMapper.selectByUserIdAndFriendId(userId, friendId);

        if (friend != null) {
            // 如果已存在（无论是 active 还是 deleted），更新为 active 状态
            friend.setDeleted(0);
            friend.setSequenceId(getNextSequenceId(userId));
            friend.setFriendNickname(friendNickname);
            friend.setStatus(1);
            friendMapper.updateById(friend);
        } else {
            // 只有不存在时才插入
            friend = new Friend();
            friend.setUserId(userId);
            friend.setFriendId(friendId);
            friend.setFriendNickname(friendNickname);
            friend.setStatus(1);
            friend.setSequenceId(getNextSequenceId(userId));
            friendMapper.insert(friend);
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
        wrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, request.getFriendId()).eq(Friend::getDeleted, 0);
        Friend friend = friendMapper.selectOne(wrapper);
        if (friend != null) {
            if (request.getFriendNickname() != null)
                friend.setFriendNickname(request.getFriendNickname());
            if (request.getFriendGroup() != null)
                friend.setFriendGroup(request.getFriendGroup());
            if (request.getTags() != null)
                friend.setTags(request.getTags());
            friend.setSequenceId(getNextSequenceId(userId));
            friendMapper.updateById(friend);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendId) {
        // 更新 user 1 的 sequence_id 并标记删除
        LambdaQueryWrapper<Friend> w1 = new LambdaQueryWrapper<>();
        w1.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId).eq(Friend::getDeleted, 0);
        Friend f1 = friendMapper.selectOne(w1);
        if (f1 != null) {
            f1.setSequenceId(getNextSequenceId(userId));
            f1.setDeleted(1);
            friendMapper.updateById(f1);
        }

        // 更新 user 2 的 sequence_id 并标记删除
        LambdaQueryWrapper<Friend> w2 = new LambdaQueryWrapper<>();
        w2.eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId).eq(Friend::getDeleted, 0);
        Friend f2 = friendMapper.selectOne(w2);
        if (f2 != null) {
            f2.setSequenceId(getNextSequenceId(friendId));
            f2.setDeleted(1);
            friendMapper.updateById(f2);
        }

        // 推送通知，触发增量同步
        java.util.Map<String, Object> delData1 = new java.util.HashMap<>();
        delData1.put("friendId", friendId);
        webSocketHandler.sendMessageToUser(userId,
                new org.linxin.server.websocket.WebSocketMessage("friend_delete", delData1));

        java.util.Map<String, Object> delData2 = new java.util.HashMap<>();
        delData2.put("friendId", userId);
        webSocketHandler.sendMessageToUser(friendId,
                new org.linxin.server.websocket.WebSocketMessage("friend_delete", delData2));
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>().eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                        .eq(Friend::getDeleted, 0)) > 0;
    }
    @Override
    public List<FriendVO> syncFriends(Long userId, Long lastSequenceId) {
        // 0. 自愈逻辑：如果前端传来的序列号比后端现有的最大值还要大，说明前端数据不一致（可能是后端重置过）
        // 此时强制从 0 开始同步
        Long maxSeq = friendMapper.selectMaxSequenceId(userId);
        if (maxSeq != null && lastSequenceId > maxSeq) {
            lastSequenceId = 0L;
        }

        // 1. 如果 lastSequenceId 为 0，说明是全量同步或初次同步，强制修复可能缺失的 sequenceId
        if (lastSequenceId == 0) {
            // ... (保持现有修复逻辑)

            // 注意：需要包括逻辑删除的记录一起修复，否则它们永远不会被同步给客户端（导致客户端无法得知删除状态）
            List<Friend> allRecords = friendMapper.selectAllRecordsByUserId(userId);

            for (Friend f : allRecords) {
                if (f.getSequenceId() == null || f.getSequenceId() == 0) {
                    f.setSequenceId(getNextSequenceId(userId));
                    friendMapper.updateById(f);
                }
            }
        }

        // 1. 拉取 sequence_id > lastSequenceId 的所有记录
        // 注意：增量同步必须包含 deleted=1 的记录，否则客户端无法同步删除操作
        return friendMapper.selectSyncRecords(userId, lastSequenceId);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSystemFriend(Long userId, Long systemUserId) {
        User systemUser = userMapper.selectById(systemUserId);
        if (systemUser != null) {
            insertOrRestoreFriend(userId, systemUserId, systemUser.getNickname());
        }
    }
}
