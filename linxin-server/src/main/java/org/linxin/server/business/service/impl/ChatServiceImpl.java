package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
import org.linxin.server.business.service.IAgentService;
import org.linxin.server.business.vo.ConversationVO;
import org.linxin.server.business.vo.MessageVO;
import org.linxin.server.common.constant.SendStatus;
import org.linxin.server.websocket.WebSocketHandler;
import org.linxin.server.websocket.WebSocketMessage;
import org.linxin.server.ai.service.AIService;
import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements IChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ChatConverter chatConverter;
    private final WebSocketHandler webSocketHandler;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final org.linxin.server.business.mapper.MessageStatusMapper messageStatusMapper;
    private final AIService aiService;
    private final IAgentService agentService;

    public ChatServiceImpl(
            ConversationMapper conversationMapper,
            MessageMapper messageMapper,
            UserMapper userMapper,
            ChatConverter chatConverter,
            @Lazy WebSocketHandler webSocketHandler,
            GroupMapper groupMapper,
            GroupMemberMapper groupMemberMapper,
            org.linxin.server.business.mapper.MessageStatusMapper messageStatusMapper,
            AIService aiService,
            IAgentService agentService) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.chatConverter = chatConverter;
        this.webSocketHandler = webSocketHandler;
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.messageStatusMapper = messageStatusMapper;
        this.aiService = aiService;
        this.agentService = agentService;
    }

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
        // AI 助手 (999) 特殊处理
        if (peerId == 999L) {
            LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getPeerId, 999L);
            Conversation conversation = conversationMapper.selectOne(wrapper);
            if (conversation == null) {
                conversation = new Conversation();
                conversation.setUserId(userId);
                conversation.setPeerId(999L);
                conversation.setPeerNickname("AI 助手");
                conversation.setPeerAvatar("");
                conversation.setUnreadCount(0);
                conversation.setTopStatus(0);
                conversation.setMuteStatus(0);
                conversationMapper.insert(conversation);
            }
            return conversation;
        }

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
        if (request.getReceiverId() != null && request.getReceiverId() == 999L) {
            return handleAIChat(senderId, request);
        }

        User receiver = userMapper.selectById(request.getReceiverId());
        if (receiver == null) {
            throw new RuntimeException("接收者不存在");
        }
        
        Conversation senderConversation = getOrCreateConversation(senderId, request.getReceiverId());
        Conversation receiverConversation = getOrCreateConversation(request.getReceiverId(), senderId);
        
        LocalDateTime sendTime = LocalDateTime.now();
        Message message = createMessageEntity(senderConversation.getId(), senderId, request.getReceiverId(), request, sendTime);
        messageMapper.insert(message);
        
        Message receiverMessage = createMessageEntity(receiverConversation.getId(), senderId, request.getReceiverId(), request, sendTime);
        messageMapper.insert(receiverMessage);
        
        updateSenderConversation(senderConversation, message, senderId);
        updateReceiverConversation(receiverConversation, receiverMessage, senderId);
        saveMessageStatus(receiverMessage.getId(), request.getReceiverId());
        
        // 推送给接收方
        MessageVO messageVO = chatConverter.toVO(receiverMessage);
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            messageVO.setSenderNickname(sender.getNickname());
            messageVO.setSenderAvatar(sender.getAvatar());
        }
        webSocketHandler.sendMessageToUser(request.getReceiverId(), new WebSocketMessage("new_message", messageVO));
        return message;
    }

    private Message handleAIChat(Long userId, SendMessageRequest request) {
        Conversation conversation = getOrCreateConversation(userId, 999L);
        LocalDateTime now = LocalDateTime.now();

        // 1. 保存用户的消息
        Message userMsg = new Message();
        userMsg.setConversationId(conversation.getId());
        userMsg.setSenderId(userId);
        userMsg.setReceiverId(999L);
        userMsg.setContent(request.getContent());
        userMsg.setSendTime(now);
        userMsg.setSendStatus(SendStatus.SENT);
        messageMapper.insert(userMsg);
        updateSenderConversation(conversation, userMsg, userId);

        // 2. 调用 AI 服务解析意图
        ChatRequest aiRequest = new ChatRequest();
        aiRequest.setContent(request.getContent());
        aiRequest.setUserId(userId);
        
        // 异步执行 AI 逻辑，此处为了 MVP 简单起见直接同步调用或由独立线程处理
        new Thread(() -> {
            try {
                ChatResponse response = aiService.processUserInput(aiRequest);
                
                // 3. 处理 AI 的回复文本
                Message aiReply = new Message();
                aiReply.setConversationId(conversation.getId());
                aiReply.setSenderId(999L);
                aiReply.setReceiverId(userId);
                aiReply.setContent(response.getReply());
                aiReply.setSendTime(LocalDateTime.now());
                aiReply.setSendStatus(SendStatus.SENT);
                messageMapper.insert(aiReply);
                updateSenderConversation(conversation, aiReply, 999L);
                
                // 推送回复给用户
                webSocketHandler.sendMessageToUser(userId, new WebSocketMessage("new_message", chatConverter.toVO(aiReply)));

                // 4. 如果有 Tool Calls，执行工具
                if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
                    for (var toolCall : response.getToolCalls()) {
                        agentService.callTool(userId, toolCall.getFunctionName(), toolCall.getArguments(), "内置AI小助手");
                    }
                }
            } catch (Exception e) {
                log.error("AI Chat processing error", e);
            }
        }).start();

        return userMsg;
    }

    private Message createMessageEntity(Long convId, Long senderId, Long receiverId, SendMessageRequest request, LocalDateTime time) {
        Message m = new Message();
        m.setConversationId(convId);
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setMessageType(request.getMessageType());
        m.setContent(request.getContent());
        m.setExtra(request.getExtra());
        m.setSendStatus(SendStatus.SENT);
        m.setSendTime(time);
        return m;
    }

    @Override
    public IPage<MessageVO> getMessageList(Long conversationId, Integer pageNum, Integer pageSize) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId).orderByDesc(Message::getSendTime);
        IPage<Message> messagePage = messageMapper.selectPage(page, wrapper);
        return messagePage.convert(message -> {
            MessageVO vo = chatConverter.toVO(message);
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            } else if (message.getSenderId() == 999L) {
                vo.setSenderNickname("AI 助手");
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
                .eq(Message::getDeleted, 0).orderByDesc(Message::getSendTime);
        Page<Message> messagePage = messageMapper.selectPage(pageParam, wrapper);
        return messagePage.convert(message -> {
            MessageVO vo = chatConverter.toVO(message);
            User sender = userMapper.selectById(message.getSenderId());
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
            webSocketHandler.sendMessageToUser(userId, new WebSocketMessage("read_status", Map.of("conversationId", conversationId, "unreadCount", 0)));
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

    @Override
    public IPage<MessageVO> getGroupMessageList(Long groupId, Long userId, Integer pageNum, Integer pageSize) {
        // 验证是否是群成员
        if (getGroupMember(groupId, userId) == null) {
            throw new RuntimeException("非群成员无法查看消息");
        }

        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getGroupId, groupId).orderByDesc(Message::getSendTime);
        IPage<Message> messagePage = messageMapper.selectPage(page, wrapper);

        return messagePage.convert(message -> {
            MessageVO vo = chatConverter.toVO(message);
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
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

    private void updateSenderConversation(Conversation conversation, Message message, Long senderId) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(truncateContent(message.getContent(), 50));
        conversation.setLastMessageType(message.getMessageType());
        conversation.setLastMessageTime(message.getSendTime());
        conversationMapper.updateById(conversation);
    }

    private void updateReceiverConversation(Conversation conversation, Message message, Long senderId) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Conversation> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(Conversation::getId, conversation.getId())
                .set(Conversation::getLastMessageId, message.getId())
                .set(Conversation::getLastMessageContent, truncateContent(message.getContent(), 50))
                .set(Conversation::getLastMessageType, message.getMessageType())
                .set(Conversation::getLastMessageTime, message.getSendTime());
        if (conversation.getMuteStatus() == 0) updateWrapper.setSql("unread_count = unread_count + 1");
        conversationMapper.update(null, updateWrapper);
    }

    private void saveMessageStatus(Long messageId, Long userId) {
        org.linxin.server.business.entity.MessageStatus status = new org.linxin.server.business.entity.MessageStatus();
        status.setMessageId(messageId);
        status.setUserId(userId);
        status.setReadStatus(0);
        status.setCreateTime(LocalDateTime.now());
        messageStatusMapper.insert(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message sendGroupMessage(Long senderId, SendMessageRequest request) {
        Long groupId = request.getGroupId();
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) throw new RuntimeException("群组不存在");
        List<GroupMember> members = groupMemberMapper.selectList(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId).eq(GroupMember::getDeleted, 0));
        LocalDateTime sendTime = LocalDateTime.now();
        Message senderMsg = null;
        for (GroupMember member : members) {
            Conversation conversation = getOrCreateGroupConversation(member.getUserId(), groupId, group.getName());
            Message message = new Message();
            message.setConversationId(conversation.getId());
            message.setSenderId(senderId);
            message.setReceiverId(0L); 
            message.setGroupId(groupId);
            message.setMessageType(request.getMessageType());
            message.setContent(request.getContent());
            message.setSendStatus(SendStatus.SENT);
            message.setSendTime(sendTime);
            messageMapper.insert(message);
            if (member.getUserId().equals(senderId)) {
                senderMsg = message;
                updateSenderConversation(conversation, message, senderId);
            } else {
                updateReceiverConversation(conversation, message, senderId);
                saveMessageStatus(message.getId(), member.getUserId());
                MessageVO messageVO = chatConverter.toVO(message);
                User sender = userMapper.selectById(senderId);
                if (sender != null) {
                    messageVO.setSenderNickname(sender.getNickname());
                    messageVO.setSenderAvatar(sender.getAvatar());
                }
                messageVO.setGroupId(groupId);
                messageVO.setConversationType(1);
                webSocketHandler.sendMessageToUser(member.getUserId(), new WebSocketMessage("group_message", messageVO));
            }
        }
        return senderMsg;
    }

    private Conversation getOrCreateGroupConversation(Long userId, Long groupId, String groupName) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getGroupId, groupId).eq(Conversation::getType, 1);
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setPeerId(0L);
            conversation.setPeerNickname(groupName);
            conversation.setType(1);
            conversation.setGroupId(groupId);
            conversation.setUnreadCount(0);
            conversation.setTopStatus(0);
            conversation.setMuteStatus(0);
            conversation.setCreateTime(LocalDateTime.now());
            conversationMapper.insert(conversation);
        }
        return conversation;
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }

    @Override
    public List<MessageVO> syncMessages(Long userId, Long lastSequenceId) {
        List<Long> groupIds = groupMemberMapper.selectList(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getUserId, userId).eq(GroupMember::getDeleted, 0))
                .stream().map(GroupMember::getGroupId).collect(Collectors.toList());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Message::getSequenceId, lastSequenceId)
               .and(w -> {
                   w.eq(Message::getSenderId, userId).or().eq(Message::getReceiverId, userId);
                   if (!groupIds.isEmpty()) w.or().in(Message::getGroupId, groupIds);
               }).orderByAsc(Message::getSequenceId);
        List<Message> messages = messageMapper.selectList(wrapper);
        return messages.stream().map(m -> {
            MessageVO vo = chatConverter.toVO(m);
            User sender = userMapper.selectById(m.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            } else if (m.getSenderId() == 999L) {
                vo.setSenderNickname("AI 助手");
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
