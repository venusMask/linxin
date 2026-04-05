package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.entity.User;
import org.linxin.server.business.mapper.MessageMapper;
import org.linxin.server.business.model.request.SendMessageRequest;
import org.linxin.server.business.service.IMessageService;
import org.linxin.server.websocket.WebSocketHandler;
import org.linxin.server.websocket.WebSocketMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    private final WebSocketHandler webSocketHandler;
    private final org.linxin.server.business.mapper.UserMapper userMapper;
    private final org.linxin.server.business.converter.ChatConverter chatConverter;

    // MVP: 使用原子类模拟全局递增序列号 (实际生产应使用 Redis 或分布式 ID)
    private static final AtomicLong sequenceGenerator = new AtomicLong(10000);

    @Override
    public Long getNextSequenceId() {
        return sequenceGenerator.incrementAndGet();
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
        Message message = new Message();
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
        org.linxin.server.business.vo.MessageVO vo = chatConverter.toVO(message);
        User sender = userMapper.selectById(senderId);
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
