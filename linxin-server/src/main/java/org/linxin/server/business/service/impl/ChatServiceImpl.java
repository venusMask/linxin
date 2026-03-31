package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.linxin.server.business.converter.ChatConverter;
import org.linxin.server.business.entity.Conversation;
import org.linxin.server.business.entity.Group;
import org.linxin.server.business.entity.GroupMember;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.ConversationMapper;
import org.linxin.server.business.mapper.GroupMapper;
import org.linxin.server.business.mapper.GroupMemberMapper;
import org.linxin.server.business.mapper.MessageMapper;
import org.linxin.server.business.mapper.UserMapper;
import org.linxin.server.business.model.request.SendMessageRequest;
import org.linxin.server.business.service.IChatService;
import org.linxin.server.business.vo.ConversationVO;
import org.linxin.server.business.vo.MessageVO;
import org.linxin.server.common.constant.SendStatus;
import org.linxin.server.websocket.WebSocketHandler;
import org.linxin.server.websocket.WebSocketMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ChatConverter chatConverter;
    private final WebSocketHandler webSocketHandler;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;

    @Override
    public IPage<ConversationVO> getConversationList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Conversation> pageParam = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
                .eq(Conversation::getDeleted, 0)
                .orderByDesc(Conversation::getTopStatus)
                .orderByDesc(Conversation::getLastMessageTime);
        
        Page<Conversation> conversationPage = conversationMapper.selectPage(pageParam, wrapper);
        
        return conversationPage.convert(chatConverter::toVO);
    }

    @Override
    public Conversation getOrCreateConversation(Long userId, Long peerId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getPeerId, peerId);
        Conversation conversation = conversationMapper.selectOne(wrapper);
        
        if (conversation == null) {
            User peer = userMapper.selectById(peerId);
            if (peer == null) {
                throw new RuntimeException("用户不存在");
            }
            
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setPeerId(peerId);
            conversation.setPeerNickname(peer.getNickname());
            conversation.setPeerAvatar(peer.getAvatar());
            conversation.setUnreadCount(0);
            conversation.setTopStatus(0);
            conversation.setMuteStatus(0);
            conversationMapper.insert(conversation);
        }
        return conversation;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Message sendMessage(Long senderId, SendMessageRequest request) {
        User receiver = userMapper.selectById(request.getReceiverId());
        if (receiver == null) {
            throw new RuntimeException("接收者不存在");
        }
        
        Conversation senderConversation = getOrCreateConversation(senderId, request.getReceiverId());
        Conversation receiverConversation = getOrCreateConversation(request.getReceiverId(), senderId);
        
        LocalDateTime sendTime = LocalDateTime.now();
        
        Message message = new Message();
        message.setConversationId(senderConversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(request.getReceiverId());
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setExtra(request.getExtra());
        message.setSendStatus(SendStatus.SENT);
        message.setSendTime(sendTime);
        messageMapper.insert(message);
        
        Message receiverMessage = new Message();
        receiverMessage.setConversationId(receiverConversation.getId());
        receiverMessage.setSenderId(senderId);
        receiverMessage.setReceiverId(request.getReceiverId());
        receiverMessage.setMessageType(request.getMessageType());
        receiverMessage.setContent(request.getContent());
        receiverMessage.setExtra(request.getExtra());
        receiverMessage.setSendStatus(SendStatus.SENT);
        receiverMessage.setSendTime(sendTime);
        messageMapper.insert(receiverMessage);
        
        updateSenderConversation(senderConversation, message, senderId);
        updateReceiverConversation(receiverConversation, receiverMessage, senderId);
        
        // 构建消息VO用于推送
        MessageVO messageVO = chatConverter.toVO(receiverMessage);
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            messageVO.setSenderNickname(sender.getNickname());
            messageVO.setSenderAvatar(sender.getAvatar());
        }
        
        // 通过WebSocket推送给接收方
        webSocketHandler.sendMessageToUser(request.getReceiverId(),
            new WebSocketMessage("new_message", messageVO));

        return message;
    }

    @Override
    public IPage<MessageVO> getMessageList(Long conversationId, Integer pageNum, Integer pageSize) {
        Page<MessageVO> page = new Page<>(pageNum, pageSize);
        IPage<Message> messagePage = messageMapper.selectMessagePage(page, conversationId);

        Set<Long> senderIds = messagePage.getRecords().stream()
                .map(Message::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return messagePage.convert(message -> {
            MessageVO vo = chatConverter.toVO(message);
            User sender = userMap.get(message.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            return vo;
        });
    }

    @Override
    public IPage<MessageVO> getMessagesBetweenUsers(Long userId, Long peerId, Integer pageNum, Integer pageSize) {
        Page<Message> pageParam = new Page<>(pageNum, pageSize, 100);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getSenderId, userId).eq(Message::getReceiverId, peerId)
                .or().eq(Message::getSenderId, peerId).eq(Message::getReceiverId, userId))
                .eq(Message::getDeleted, 0)
                .orderByDesc(Message::getSendTime);

        Page<Message> messagePage = messageMapper.selectPage(pageParam, wrapper);

        Set<Long> senderIds = messagePage.getRecords().stream()
                .map(Message::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return messagePage.convert(message -> {
            MessageVO vo = chatConverter.toVO(message);
            User sender = userMap.get(message.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            return vo;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markMessagesAsRead(Long userId, Long conversationId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getId, conversationId);
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation != null) {
            conversation.setUnreadCount(0);
            conversationMapper.updateById(conversation);
            
            // 推送已读状态更新
            webSocketHandler.sendMessageToUser(userId, 
                new WebSocketMessage("read_status", 
                    Map.of("conversationId", conversationId, "unreadCount", 0)));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void toggleTop(Long userId, Long conversationId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getId, conversationId);
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation != null) {
            conversation.setTopStatus(conversation.getTopStatus() == 0 ? 1 : 0);
            conversationMapper.updateById(conversation);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void toggleMute(Long userId, Long conversationId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getId, conversationId);
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation != null) {
            conversation.setMuteStatus(conversation.getMuteStatus() == 0 ? 1 : 0);
            conversationMapper.updateById(conversation);
        }
    }

    private void updateSenderConversation(Conversation conversation, Message message, Long senderId) {
        User sender = userMapper.selectById(senderId);
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(truncateContent(message.getContent(), 50));
        conversation.setLastMessageType(message.getMessageType());
        conversation.setLastMessageTime(message.getSendTime());
        if (sender != null) {
            conversation.setPeerNickname(sender.getNickname());
            conversation.setPeerAvatar(sender.getAvatar());
        }
        conversationMapper.updateById(conversation);
    }

    private void updateReceiverConversation(Conversation conversation, Message message, Long senderId) {
        User sender = userMapper.selectById(senderId);
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(truncateContent(message.getContent(), 50));
        conversation.setLastMessageType(message.getMessageType());
        conversation.setLastMessageTime(message.getSendTime());
        if (conversation.getMuteStatus() == 0) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        if (sender != null) {
            conversation.setPeerNickname(sender.getNickname());
            conversation.setPeerAvatar(sender.getAvatar());
        }
        conversationMapper.updateById(conversation);
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message sendGroupMessage(Long senderId, SendMessageRequest request) {
        Long groupId = request.getGroupId();
        if (groupId == null) {
            throw new RuntimeException("群ID不能为空");
        }

        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new RuntimeException("群组不存在");
        }

        GroupMember senderMember = getGroupMember(groupId, senderId);
        if (senderMember == null) {
            throw new RuntimeException("你不是群成员");
        }

        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getDeleted, 0);
        List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);

        LambdaQueryWrapper<Conversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(Conversation::getGroupId, groupId)
                .eq(Conversation::getDeleted, 0);
        List<Conversation> conversations = conversationMapper.selectList(convWrapper);
        Map<Long, Conversation> convMap = conversations.stream()
                .collect(Collectors.toMap(Conversation::getUserId, c -> c));

        LocalDateTime sendTime = LocalDateTime.now();

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(0L);
        message.setGroupId(groupId);
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setExtra(request.getExtra());
        message.setSendStatus(SendStatus.SENT);
        message.setSendTime(sendTime);
        messageMapper.insert(message);

        User sender = userMapper.selectById(senderId);
        String senderNickname = sender != null ? sender.getNickname() : "未知";
        String senderAvatar = sender != null ? sender.getAvatar() : null;

        for (GroupMember member : members) {
            Conversation conversation = convMap.get(member.getUserId());
            if (conversation == null) {
                continue;
            }

            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageContent(truncateContent(request.getContent(), 50));
            conversation.setLastMessageType(request.getMessageType());
            conversation.setLastMessageTime(sendTime);
            if (!member.getUserId().equals(senderId) && conversation.getMuteStatus() == 0) {
                conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            }
            conversationMapper.updateById(conversation);
        }

        MessageVO messageVO = chatConverter.toVO(message);
        messageVO.setSenderNickname(senderNickname);
        messageVO.setSenderAvatar(senderAvatar);
        messageVO.setGroupId(groupId);
        messageVO.setConversationType(1);

        final Message finalMessage = message;
        members.stream()
                .filter(m -> !m.getUserId().equals(senderId))
                .forEach(member -> {
                    webSocketHandler.sendMessageToUser(member.getUserId(),
                            new WebSocketMessage("group_message", messageVO));
                });

        webSocketHandler.sendMessageToUser(senderId,
                new WebSocketMessage("group_message", messageVO));

        return message;
    }

    @Override
    public IPage<MessageVO> getGroupMessageList(Long groupId, Long userId, Integer pageNum, Integer pageSize) {
        GroupMember member = getGroupMember(groupId, userId);
        if (member == null) {
            throw new RuntimeException("你不是群成员");
        }

        LambdaQueryWrapper<Conversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(Conversation::getUserId, userId)
                .eq(Conversation::getGroupId, groupId)
                .eq(Conversation::getDeleted, 0);
        Conversation conversation = conversationMapper.selectOne(convWrapper);
        if (conversation == null) {
            return new Page<>();
        }

        Page<MessageVO> page = new Page<>(pageNum, pageSize);
        IPage<Message> messagePage = messageMapper.selectMessagePage(page, conversation.getId());

        Set<Long> senderIds = messagePage.getRecords().stream()
                .map(Message::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return messagePage.convert(message -> {
            MessageVO vo = chatConverter.toVO(message);
            User sender = userMap.get(message.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            vo.setGroupId(groupId);
            vo.setConversationType(1);
            return vo;
        });
    }

    private GroupMember getGroupMember(Long groupId, Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getDeleted, 0);
        return groupMemberMapper.selectOne(wrapper);
    }
}
