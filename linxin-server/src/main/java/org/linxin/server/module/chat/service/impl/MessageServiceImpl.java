package org.linxin.server.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.linxin.server.module.chat.entity.Message;
import org.linxin.server.module.chat.mapper.MessageMapper;
import org.linxin.server.module.chat.model.request.SendMessageRequest;
import org.linxin.server.module.chat.service.IMessageService;
import org.linxin.server.websocket.WebSocketHandler;
import org.linxin.server.websocket.WebSocketMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    private final WebSocketHandler webSocketHandler;
    private final org.linxin.server.module.user.mapper.UserMapper userMapper;
    private final org.linxin.server.module.chat.mapper.ConversationMapper conversationMapper;
    private final org.linxin.server.module.chat.converter.ChatConverter chatConverter;
    private final org.linxin.server.common.util.SnowflakeIdWorker snowflakeIdWorker;

    @Override
    public Long getNextSequenceId() {
        return snowflakeIdWorker.nextId();
    }

    @Override
    public Message sendMessage(Long userId, SendMessageRequest request) {
        Message message = new Message();
        message.setSenderId(userId);
        message.setReceiverId(request.getReceiverId());
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : 1);
        message.setConversationId(request.getConversationId());
        message.setSendTime(LocalDateTime.now());
        message.setSendStatus(1);
        message.setIsAi(false); // 用户手动发送
        message.setSequenceId(getNextSequenceId());

        this.save(message);

        // 推送给接收方
        webSocketHandler.sendMessageToUser(request.getReceiverId(),
                new WebSocketMessage("new_message", message));

        return message;
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
    }

    @Override
    public Message sendAgentMessage(Long senderId, Long receiverId, String content, String agentName) {
        // 查找或创建会话 (Agent 消息通常属于用户与对方的会话)
        LambdaQueryWrapper<org.linxin.server.module.chat.entity.Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(org.linxin.server.module.chat.entity.Conversation::getUserId, senderId)
                .eq(org.linxin.server.module.chat.entity.Conversation::getPeerId, receiverId);
        org.linxin.server.module.chat.entity.Conversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            // 如果不存在，尝试查找群组会话
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(org.linxin.server.module.chat.entity.Conversation::getUserId, senderId)
                    .eq(org.linxin.server.module.chat.entity.Conversation::getGroupId, receiverId);
            conversation = conversationMapper.selectOne(wrapper);
        }

        Message message = new Message();
        message.setConversationId(conversation != null ? conversation.getId() : 0L); // 如果找不到，暂时用 0L 兜底，但理想情况应已存在
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(1);
        message.setSendTime(LocalDateTime.now());
        message.setSendStatus(1);
        message.setIsAi(true); // AI 发送
        message.setSenderType(agentName); // 存储 Agent 名称
        message.setSequenceId(getNextSequenceId());

        this.save(message);

        // 推送给双方
        org.linxin.server.module.chat.vo.MessageVO vo = chatConverter.toVO(message);
        org.linxin.server.module.user.entity.User sender = userMapper.selectById(senderId);
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
            vo.setUserType(sender.getUserType());
        }

        WebSocketMessage wsMsg = new WebSocketMessage("new_message", vo);
        webSocketHandler.sendMessageToUser(senderId, wsMsg);
        webSocketHandler.sendMessageToUser(receiverId, wsMsg);

        return message;
    }
}
