package org.linxin.server.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.linxin.server.business.entity.Conversation;
import org.linxin.server.business.entity.Message;
import org.linxin.server.business.model.request.SendMessageRequest;
import org.linxin.server.business.vo.ConversationVO;
import org.linxin.server.business.vo.MessageVO;

public interface IChatService {

    IPage<ConversationVO> getConversationList(Long userId, Integer pageNum, Integer pageSize);

    Conversation getOrCreateConversation(Long userId, Long peerId);

    Message sendMessage(Long senderId, SendMessageRequest request);

    IPage<MessageVO> getMessageList(Long conversationId, Integer pageNum, Integer pageSize);

    IPage<MessageVO> getMessagesBetweenUsers(Long userId, Long peerId, Integer pageNum, Integer pageSize);

    void markMessagesAsRead(Long userId, Long conversationId);

    void toggleTop(Long userId, Long conversationId);

    void toggleMute(Long userId, Long conversationId);

    Message sendGroupMessage(Long senderId, SendMessageRequest request);

    IPage<MessageVO> getGroupMessageList(Long groupId, Long userId, Integer pageNum, Integer pageSize);
}
