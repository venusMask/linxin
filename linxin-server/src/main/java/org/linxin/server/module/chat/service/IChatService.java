package org.linxin.server.module.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.linxin.server.module.chat.entity.Conversation;
import org.linxin.server.module.chat.entity.Message;
import org.linxin.server.module.chat.model.request.SendMessageRequest;
import org.linxin.server.module.chat.vo.ConversationVO;
import org.linxin.server.module.chat.vo.MessageVO;

public interface IChatService {

    IPage<ConversationVO> getConversationList(Long userId, Integer pageNum, Integer pageSize);

    Conversation getOrCreateConversation(Long userId, Long peerId);

    ConversationVO getOrCreateAIConversation(Long userId);

    Message sendMessage(Long senderId, SendMessageRequest request);

    IPage<MessageVO> getMessageList(Long conversationId, Integer pageNum, Integer pageSize);

    IPage<MessageVO> getMessagesBetweenUsers(Long userId, Long peerId, Integer pageNum, Integer pageSize);

    void markMessagesAsRead(Long userId, Long conversationId);

    void toggleTop(Long userId, Long conversationId);

    void toggleMute(Long userId, Long conversationId);

    Message sendGroupMessage(Long senderId, SendMessageRequest request);

    IPage<MessageVO> getGroupMessageList(Long groupId, Long userId, Integer pageNum, Integer pageSize);

    java.util.List<MessageVO> syncMessages(Long userId, Long lastSequenceId);

    /**
     * 校验会话是否属于指定用户（防 IDOR 越权）
     */
    boolean isConversationOwner(Long userId, Long conversationId);
}
