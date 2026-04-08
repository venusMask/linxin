package org.linxin.server.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.linxin.server.common.constant.SendStatus;
import org.linxin.server.module.chat.converter.ChatConverter;
import org.linxin.server.module.chat.entity.Conversation;
import org.linxin.server.module.chat.entity.Message;
import org.linxin.server.module.chat.mapper.ConversationMapper;
import org.linxin.server.module.chat.mapper.MessageMapper;
import org.linxin.server.module.chat.service.IMessageService;
import org.linxin.server.module.chat.vo.MessageVO;
import org.linxin.server.module.user.entity.User;
import org.linxin.server.module.user.mapper.UserMapper;
import org.linxin.server.websocket.WebSocketHandler;
import org.linxin.server.websocket.WebSocketMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息服务实现类
 * 
 * 核心职能：sequenceId 生成及代发消息。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    private final org.linxin.server.common.util.SnowflakeIdWorker snowflakeIdWorker;
    private final WebSocketHandler webSocketHandler;
    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;
    private final ChatConverter chatConverter;

    @Override
    public Long getNextSequenceId() {
        return snowflakeIdWorker.nextId();
    }

    @Override
    public List<Message> getMessages(Long conversationId, int pageNum, int pageSize) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getSequenceId);
        return this.page(page, wrapper).getRecords();
    }

    @Override
    public void markRead(Long userId, Long conversationId) {
        // 标记已读逻辑已移动到 ChatServiceImpl
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Message sendAgentMessage(Long senderId, Long receiverId, String content, String agentName) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 保存消息（读扩散：conversationId = 0）
        Message message = new Message();
        message.setConversationId(0L);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(1);
        message.setSendTime(now);
        message.setSendStatus(SendStatus.SENT);
        message.setIsAi(true);
        message.setSenderType(agentName);
        message.setSequenceId(getNextSequenceId());
        this.save(message);

        // 2. 更新双方会话状态（借用内部逻辑，不加未读数）
        updateConversation(senderId, receiverId, message);
        updateConversation(receiverId, senderId, message);

        // 3. 推送给接收方
        MessageVO vo = chatConverter.toVO(message);
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
            vo.setUserType(sender.getUserType());
        }

        WebSocketMessage wsMsg = new WebSocketMessage("new_message", vo);
        webSocketHandler.sendMessageToUser(receiverId, wsMsg);

        return message;
    }

    private void updateConversation(Long userId, Long peerId, Message message) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId).eq(Conversation::getPeerId, peerId);
        Conversation conv = conversationMapper.selectOne(wrapper);
        if (conv != null) {
            conv.setLastMessageId(message.getId());
            conv.setLastMessageContent(message.getContent());
            conv.setLastMessageType(message.getMessageType());
            conv.setLastMessageTime(message.getSendTime());
            conversationMapper.updateById(conv);
        }
    }
}
