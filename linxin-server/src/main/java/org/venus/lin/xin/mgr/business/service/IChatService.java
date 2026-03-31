package org.venus.lin.xin.mgr.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.venus.lin.xin.mgr.business.entity.Conversation;
import org.venus.lin.xin.mgr.business.entity.Message;
import org.venus.lin.xin.mgr.business.model.request.SendMessageRequest;
import org.venus.lin.xin.mgr.business.vo.ConversationVO;
import org.venus.lin.xin.mgr.business.vo.MessageVO;

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
