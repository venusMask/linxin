package org.linxin.server.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.ai.dto.ChatRequest;
import org.linxin.server.ai.dto.ChatResponse;
import org.linxin.server.ai.service.AIService;
import org.linxin.server.common.constant.SendStatus;
import org.linxin.server.module.auth.service.IAgentService;
import org.linxin.server.module.chat.converter.ChatConverter;
import org.linxin.server.module.chat.entity.Conversation;
import org.linxin.server.module.chat.entity.Message;
import org.linxin.server.module.chat.mapper.ConversationMapper;
import org.linxin.server.module.chat.mapper.MessageMapper;
import org.linxin.server.module.chat.model.request.SendMessageRequest;
import org.linxin.server.module.chat.service.IChatService;
import org.linxin.server.module.chat.vo.ConversationVO;
import org.linxin.server.module.chat.vo.MessageVO;
import org.linxin.server.module.group.entity.Group;
import org.linxin.server.module.group.entity.GroupMember;
import org.linxin.server.module.group.mapper.GroupMapper;
import org.linxin.server.module.group.mapper.GroupMemberMapper;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.mapper.UserMapper;
import org.linxin.server.websocket.WebSocketHandler;
import org.linxin.server.websocket.WebSocketMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AIService aiService;
    private final IAgentService agentService;
    private final org.linxin.server.module.chat.service.IMessageService messageService;
    private final org.linxin.server.module.contact.service.IFriendService friendService;
    private final Executor taskExecutor;

    public ChatServiceImpl(
            ConversationMapper conversationMapper,
            MessageMapper messageMapper,
            UserMapper userMapper,
            ChatConverter chatConverter,
            @Lazy WebSocketHandler webSocketHandler,
            GroupMapper groupMapper,
            GroupMemberMapper groupMemberMapper,
            AIService aiService,
            IAgentService agentService,
            org.linxin.server.module.chat.service.IMessageService messageService,
            org.linxin.server.module.contact.service.IFriendService friendService,
            @Qualifier("taskExecutor") Executor taskExecutor) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.chatConverter = chatConverter;
        this.webSocketHandler = webSocketHandler;
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.aiService = aiService;
        this.agentService = agentService;
        this.messageService = messageService;
        this.friendService = friendService;
        this.taskExecutor = taskExecutor;
    }

    private User getSystemAIUser() {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserType, 1).last("LIMIT 1"));
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
        User aiUser = getSystemAIUser();
        Long aiUserId = aiUser != null ? aiUser.getId() : null;

        // AI 助手特殊处理 (基于数据库中实际的 AI 用户 ID)
        if (aiUserId != null && peerId.equals(aiUserId)) {
            return getOrCreateAIConversationInternal(userId);
        }

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getPeerId, peerId);
        Conversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            User peer = userMapper.selectById(peerId);
            if (peer == null) {
                throw new org.linxin.server.common.exception.BusinessException("用户不存在");
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

    @Override
    public ConversationVO getOrCreateAIConversation(Long userId) {
        Conversation conversation = getOrCreateAIConversationInternal(userId);
        ConversationVO vo = chatConverter.toVO(conversation);
        vo.setUserType(1);
        return vo;
    }

    private Conversation getOrCreateAIConversationInternal(Long userId) {
        User aiUser = getSystemAIUser();
        if (aiUser == null) {
            throw new org.linxin.server.common.exception.BusinessException("系统 AI 助手尚未配置，请联系管理员");
        }
        Long aiUserId = aiUser.getId();

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getPeerId, aiUserId);
        Conversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setPeerId(aiUserId);
            conversation.setPeerNickname(aiUser.getNickname());
            conversation.setPeerAvatar(aiUser.getAvatar());
            conversation.setUnreadCount(0);
            conversation.setTopStatus(0);
            conversation.setMuteStatus(0);
            conversationMapper.insert(conversation);
        }

        return conversation;
    }

    /**
     * 发送普通消息（私聊）
     * 
     * @param senderId
     *            发送者 ID
     * @param request
     *            发送请求
     * @return 消息实体
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Message sendMessage(Long senderId, SendMessageRequest request) {
        User aiUser = getSystemAIUser();
        Long aiUserId = aiUser != null ? aiUser.getId() : null;

        if (request.getReceiverId() != null && aiUserId != null && request.getReceiverId().equals(aiUserId)) {
            return handleAIChat(senderId, request);
        }

        User receiver = userMapper.selectById(request.getReceiverId());
        if (receiver == null) {
            throw new org.linxin.server.common.exception.BusinessException("接收者不存在");
        }

        // 安全检查：核实是否为好友
        if (!friendService.isFriend(senderId, request.getReceiverId())) {
            throw new org.linxin.server.common.exception.BusinessException("对方还不是您的好友，无法发送消息");
        }

        Conversation senderConversation = getOrCreateConversation(senderId, request.getReceiverId());
        Conversation receiverConversation = getOrCreateConversation(request.getReceiverId(), senderId);

        LocalDateTime sendTime = LocalDateTime.now();
        Message message = createMessageEntity(senderConversation.getId(), senderId, request.getReceiverId(), request,
                sendTime);
        messageMapper.insert(message);

        Message receiverMessage = createMessageEntity(receiverConversation.getId(), senderId, request.getReceiverId(),
                request, sendTime);
        messageMapper.insert(receiverMessage);

        updateSenderConversation(senderConversation, message, senderId);
        updateReceiverConversation(receiverConversation, receiverMessage, senderId);
        // 移除已失效的消息状态保存逻辑

        // 推送给接收方
        MessageVO messageVO = chatConverter.toVO(receiverMessage);
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            messageVO.setSenderNickname(sender.getNickname());
            messageVO.setSenderAvatar(sender.getAvatar());
            messageVO.setUserType(sender.getUserType());
        }
        webSocketHandler.sendMessageToUser(request.getReceiverId(), new WebSocketMessage("new_message", messageVO));
        return message;
    }

    /**
     * 处理与 AI 助手的对话
     * 
     * @param userId
     *            用户 ID
     * @param request
     *            请求内容
     * @return 用户发送的消息实体
     */
    private Message handleAIChat(Long userId, SendMessageRequest request) {
        User aiUser = getSystemAIUser();
        if (aiUser == null) {
            throw new org.linxin.server.common.exception.BusinessException("AI 助手未在线，请稍后再试");
        }
        Long aiUserId = aiUser.getId();

        Conversation conversation = getOrCreateConversation(userId, aiUserId);
        LocalDateTime now = LocalDateTime.now();

        // 0. 加载历史上下文（最近 10 条）
        LambdaQueryWrapper<Message> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(Message::getConversationId, conversation.getId())
                .orderByDesc(Message::getSendTime)
                .last("LIMIT 10");
        List<Message> history = messageMapper.selectList(historyWrapper);
        java.util.Collections.reverse(history);

        List<org.linxin.server.ai.core.dto.ChatMessage> context = history.stream().map(m -> {
            String role = m.getSenderId().equals(aiUserId) ? "assistant" : "user";
            return org.linxin.server.ai.core.dto.ChatMessage.builder()
                    .role(role)
                    .content(m.getContent())
                    .build();
        }).collect(Collectors.toList());

        // 1. 保存用户的消息
        Message userMsg = new Message();
        userMsg.setConversationId(conversation.getId());
        userMsg.setSenderId(userId);
        userMsg.setReceiverId(aiUserId);
        userMsg.setContent(request.getContent());
        userMsg.setSendTime(now);
        userMsg.setSendStatus(SendStatus.SENT);
        userMsg.setIsAi(false);
        userMsg.setSequenceId(messageService.getNextSequenceId());
        messageMapper.insert(userMsg);
        updateSenderConversation(conversation, userMsg, userId);

        // 2. 调用 AI 服务
        ChatRequest aiRequest = new ChatRequest();
        aiRequest.setContent(request.getContent());
        aiRequest.setUserId(userId);

        final Long conversationId = conversation.getId();
        final String aiNickname = aiUser.getNickname();
        final String aiAvatar = aiUser.getAvatar();

        taskExecutor.execute(() -> {
            try {
                // 将上下文传递给 AI Service
                ChatResponse response = aiService.processUserInput(aiRequest, context);

                // 3. 处理 AI 的回复文本并推送
                if (response.getReply() != null && !response.getReply().isBlank()) {
                    Message aiReply = new Message();
                    aiReply.setConversationId(conversationId);
                    aiReply.setSenderId(aiUserId);
                    aiReply.setReceiverId(userId);
                    aiReply.setContent(response.getReply());
                    aiReply.setSendTime(LocalDateTime.now());
                    aiReply.setSendStatus(SendStatus.SENT);
                    aiReply.setIsAi(true);
                    aiReply.setSenderType("AI_ASSISTANT");
                    aiReply.setSequenceId(messageService.getNextSequenceId());
                    messageMapper.insert(aiReply);

                    Conversation freshConv = conversationMapper.selectById(conversationId);
                    if (freshConv != null) {
                        updateSenderConversation(freshConv, aiReply, aiUserId);
                    }

                    MessageVO aiReplyVO = chatConverter.toVO(aiReply);
                    aiReplyVO.setSenderNickname(aiNickname);
                    aiReplyVO.setSenderAvatar(aiAvatar);
                    aiReplyVO.setUserType(1);

                    webSocketHandler.sendMessageToUser(userId,
                            new WebSocketMessage("new_message", aiReplyVO));
                }
            } catch (Exception e) {
                log.error("AI Chat processing error for user {}", userId, e);
                // 向用户推送错误提示
                try {
                    Message errMsg = new Message();
                    errMsg.setConversationId(conversationId);
                    errMsg.setSenderId(aiUserId);
                    errMsg.setReceiverId(userId);
                    errMsg.setContent("⚠️ AI 服务暂时不可用，请稍后再试");
                    errMsg.setSendTime(LocalDateTime.now());
                    errMsg.setSendStatus(SendStatus.SENT);
                    errMsg.setIsAi(true);
                    errMsg.setSenderType("AI_ASSISTANT");
                    errMsg.setSequenceId(messageService.getNextSequenceId());
                    messageMapper.insert(errMsg);

                    MessageVO errVO = chatConverter.toVO(errMsg);
                    errVO.setSenderNickname(aiNickname);
                    errVO.setSenderAvatar(aiAvatar);
                    errVO.setUserType(1);
                    webSocketHandler.sendMessageToUser(userId, new WebSocketMessage("new_message", errVO));
                } catch (Exception ex) {
                    log.error("Failed to send AI error notification to user {}", userId, ex);
                }
            }
        });

        return userMsg;
    }

    private Message createMessageEntity(Long convId, Long senderId, Long receiverId, SendMessageRequest request,
            LocalDateTime time) {
        Message m = new Message();
        m.setConversationId(convId);
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setMessageType(request.getMessageType());
        m.setContent(request.getContent());
        m.setExtra(request.getExtra());
        m.setSendStatus(SendStatus.SENT);
        m.setSendTime(time);
        m.setIsAi(false);
        m.setSequenceId(messageService.getNextSequenceId());
        return m;
    }

    @Override
    public IPage<MessageVO> getMessageList(Long conversationId, Integer pageNum, Integer pageSize) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId).orderByDesc(Message::getSendTime);
        IPage<Message> messagePage = messageMapper.selectPage(page, wrapper);

        if (messagePage.getRecords().isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // 批量预加载发送者信息 (N+1 优化)
        List<Long> senderIds = messagePage.getRecords().stream()
                .map(Message::getSenderId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        User aiUser = getSystemAIUser();
        Long aiUserId = aiUser != null ? aiUser.getId() : null;

        return messagePage.convert(message -> {
            MessageVO vo = chatConverter.toVO(message);
            User sender = userMap.get(message.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
                vo.setUserType(sender.getUserType());
            } else if (aiUserId != null && message.getSenderId().equals(aiUserId)) {
                vo.setSenderNickname(aiUser.getNickname());
                vo.setUserType(1);
            }
            return vo;
        });
    }

    @Override
    public IPage<MessageVO> getMessagesBetweenUsers(Long userId, Long peerId, Integer pageNum, Integer pageSize) {
        Page<Message> pageParam = new Page<>(pageNum, pageSize, 100);

        Conversation conversation = getOrCreateConversation(userId, peerId);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversation.getId())
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
            webSocketHandler.sendMessageToUser(userId,
                    new WebSocketMessage("read_status", Map.of("conversationId", conversationId, "unreadCount", 0)));
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
            throw new org.linxin.server.common.exception.BusinessException("非群成员无法查看消息");
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
        if (conversation.getMuteStatus() == 0)
            updateWrapper.setSql("unread_count = unread_count + 1");
        conversationMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message sendGroupMessage(Long senderId, SendMessageRequest request) {
        Long groupId = request.getGroupId();
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new org.linxin.server.common.exception.BusinessException("群组不存在");
        }

        List<GroupMember> members = groupMemberMapper.selectList(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId).eq(GroupMember::getDeleted, 0));
        LocalDateTime sendTime = LocalDateTime.now();

        // 1. 读扩散优化：仅插入一条消息记录
        Message groupMsg = new Message();
        groupMsg.setConversationId(0L); // 群聊消息在读扩散模式下，conversationId 设为 0
        groupMsg.setSenderId(senderId);
        groupMsg.setReceiverId(0L);
        groupMsg.setGroupId(groupId);
        groupMsg.setMessageType(request.getMessageType());
        groupMsg.setContent(request.getContent());
        groupMsg.setSendStatus(SendStatus.SENT);
        groupMsg.setSendTime(sendTime);
        groupMsg.setIsAi(false);
        groupMsg.setSequenceId(messageService.getNextSequenceId());
        messageMapper.insert(groupMsg);

        // 2. 批量更新所有群成员的会话状态 (排除发送者，发送者的会话由专门方法更新)
        // 注意：在 MySQL-Only 环境下，这是降低 I/O 压力的关键
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Conversation> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(Conversation::getGroupId, groupId)
                .eq(Conversation::getType, 1) // 群聊类型
                .set(Conversation::getLastMessageId, groupMsg.getId())
                .set(Conversation::getLastMessageContent, truncateContent(groupMsg.getContent(), 50))
                .set(Conversation::getLastMessageType, groupMsg.getMessageType())
                .set(Conversation::getLastMessageTime, groupMsg.getSendTime())
                .setSql("unread_count = unread_count + 1");

        // 更新接收者们（不包括发送者，发送者不需要加未读数）
        updateWrapper.ne(Conversation::getUserId, senderId);
        conversationMapper.update(null, updateWrapper);

        // 更新发送者的会话（不加未读数）
        Conversation senderConv = getOrCreateGroupConversation(senderId, groupId, group.getName());
        updateSenderConversation(senderConv, groupMsg, senderId);

        // 3. 异步推送给所有在线成员
        User sender = userMapper.selectById(senderId);
        MessageVO messageVO = chatConverter.toVO(groupMsg);
        if (sender != null) {
            messageVO.setSenderNickname(sender.getNickname());
            messageVO.setSenderAvatar(sender.getAvatar());
        }
        messageVO.setGroupId(groupId);
        messageVO.setConversationType(1);

        // 使用 taskExecutor 异步推送，减少对事务耗时的影响
        taskExecutor.execute(() -> {
            for (GroupMember member : members) {
                if (!member.getUserId().equals(senderId)) {
                    webSocketHandler.sendMessageToUser(member.getUserId(),
                            new WebSocketMessage("group_message", messageVO));
                }
            }
        });

        return groupMsg;
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
        if (content == null)
            return null;
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }

    @Override
    public List<MessageVO> syncMessages(Long userId, Long lastSequenceId) {
        // 自愈逻辑：如果前端传来的序列号比后端现有的最大值还要大，说明前端数据不一致（可能是后端重置过）
        Long maxSeq = messageMapper.selectMaxSequenceId(userId);
        if (maxSeq != null && lastSequenceId > maxSeq) {
            lastSequenceId = 0L;
        }

        List<Long> conversationIds = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>().eq(Conversation::getUserId, userId)).stream()
                .map(Conversation::getId).collect(Collectors.toList());

        List<Long> groupIds = groupMemberMapper
                .selectList(new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getUserId, userId)
                        .eq(GroupMember::getDeleted, 0))
                .stream().map(GroupMember::getGroupId).collect(Collectors.toList());

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Message::getSequenceId, lastSequenceId)
                .and(w -> {
                    if (!conversationIds.isEmpty()) {
                        w.in(Message::getConversationId, conversationIds);
                    } else {
                        w.eq(Message::getConversationId, -1L);
                    }
                    if (!groupIds.isEmpty()) {
                        w.or().in(Message::getGroupId, groupIds);
                    }
                })
                .orderByAsc(Message::getSequenceId)
                .last("LIMIT 500"); // 增加单次同步限制，防止 OOM

        List<Message> messages = messageMapper.selectList(wrapper);

        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量预加载发送者信息 (N+1 优化)
        List<Long> senderIds = messages.stream().map(Message::getSenderId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        User aiUser = getSystemAIUser();
        Long aiUserId = aiUser != null ? aiUser.getId() : null;

        return messages.stream().map(m -> {
            MessageVO vo = chatConverter.toVO(m);
            User sender = userMap.get(m.getSenderId());
            if (sender != null) {
                vo.setSenderNickname(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
                vo.setUserType(sender.getUserType());
            } else if (aiUserId != null && m.getSenderId().equals(aiUserId)) {
                vo.setSenderNickname(aiUser.getNickname());
                vo.setUserType(1);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isConversationOwner(Long userId, Long conversationId) {
        if (userId == null || conversationId == null) {
            return false;
        }
        Conversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getId, conversationId)
                        .eq(Conversation::getUserId, userId));
        return conv != null;
    }
}
