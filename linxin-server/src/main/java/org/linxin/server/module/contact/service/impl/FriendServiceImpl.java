package org.linxin.server.module.contact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements IFriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final FriendApplyMapper friendApplyMapper;
    private final org.linxin.server.common.util.SnowflakeIdWorker snowflakeIdWorker;

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
                    aiVo.setId(aiUser.getId()); // 设置一个确定的 ID
                    aiVo.setFriendId(aiUser.getId());
                    aiVo.setFriendNickname("AI 助手");
                    aiVo.setNickname("AI 助手");
                    aiVo.setUsername(aiUser.getUsername());
                    aiVo.setAvatar(aiUser.getAvatar());
                    aiVo.setUserStatus(aiUser.getStatus());
                    aiVo.setSignature("我是您的AI智能助手");
                    aiVo.setUserType(1);
                    aiVo.setSequenceId(0L); // 系统内置 AI 序列号为 0
                    records.add(0, aiVo);
                }
            }
        }

        long total = friendPage.getTotal();
        // 如果 records 长度超过数据库记录，说明手动添加了 AI 助手
        if (records.size() > friendPage.getRecords().size()) {
            total += 1;
        }

        Page<FriendVO> voPage = new Page<>(pageNum, pageSize, total);
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
            f1.setSequenceId(getNextSequenceId(user1));
            friendMapper.insert(f1);

            Friend f2 = new Friend();
            f2.setUserId(user2);
            f2.setFriendId(user1);
            f2.setFriendNickname(u1.getNickname());
            f2.setStatus(1);
            f2.setSequenceId(getNextSequenceId(user2));
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
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>().eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                        .eq(Friend::getDeleted, 0)) > 0;
    }

    @Override
    public List<FriendVO> syncFriends(Long userId, Long lastSequenceId) {
        // 1. 拉取 sequence_id > lastSequenceId 的所有记录（包括已删除的，以便通知客户端删除本地存储）
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId).gt(Friend::getSequenceId, lastSequenceId)
                .orderByAsc(Friend::getSequenceId);
        List<Friend> friends = friendMapper.selectList(wrapper);

        List<FriendVO> results = friends.stream().map(f -> {
            FriendVO vo = new FriendVO();
            vo.setId(f.getId());
            vo.setUserId(f.getUserId());
            vo.setFriendId(f.getFriendId());
            vo.setFriendNickname(f.getFriendNickname());
            vo.setFriendGroup(f.getFriendGroup());
            vo.setTags(f.getTags());
            vo.setCreateTime(f.getCreateTime());
            vo.setSequenceId(f.getSequenceId());
            vo.setDeleted(f.getDeleted());

            if (f.getDeleted() == 0) {
                User user = userMapper.selectById(f.getFriendId());
                if (user != null) {
                    vo.setUsername(user.getUsername());
                    vo.setAvatar(user.getAvatar());
                    vo.setUserStatus(user.getStatus());
                    vo.setSignature(user.getSignature());
                    vo.setUserType(user.getUserType());
                    vo.setNickname(user.getNickname()); // 设置 nickname 字段
                    if (vo.getFriendNickname() == null)
                        vo.setFriendNickname(user.getNickname());
                }
            }
            return vo;
        }).collect(Collectors.toList());

        // 2. 如果是首次同步，注入 AI 助手
        if (lastSequenceId == 0) {
            User aiUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserType, 1).last("LIMIT 1"));
            if (aiUser != null) {
                boolean exists = results.stream().anyMatch(f -> aiUser.getId().equals(f.getFriendId()));
                if (!exists) {
                    FriendVO aiVo = new FriendVO();
                    aiVo.setId(aiUser.getId());
                    aiVo.setFriendId(aiUser.getId());
                    aiVo.setFriendNickname("AI 助手");
                    aiVo.setNickname("AI 助手");
                    aiVo.setUsername(aiUser.getUsername());
                    aiVo.setAvatar(aiUser.getAvatar());
                    aiVo.setUserStatus(aiUser.getStatus());
                    aiVo.setSignature("我是您的AI智能助手");
                    aiVo.setUserType(1);
                    aiVo.setSequenceId(0L);
                    aiVo.setDeleted(0);
                    results.add(0, aiVo);
                }
            }
        }

        return results;
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
