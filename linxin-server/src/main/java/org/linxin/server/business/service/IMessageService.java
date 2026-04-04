package org.linxin.server.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.model.request.SendMessageRequest;

public interface IMessageService extends IService<Message> {
    Message sendMessage(Long userId, SendMessageRequest request);

    List<Message> getMessages(Long conversationId, int pageNum, int pageSize);

    void markRead(Long userId, Long conversationId);

    Message sendAgentMessage(Long senderId, Long receiverId, String content, String agentName);
}
