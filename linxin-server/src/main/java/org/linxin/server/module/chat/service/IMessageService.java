package org.linxin.server.module.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import org.linxin.server.module.chat.entity.Message;

/**
 * 消息服务接口
 * 
 * 已精简死代码，核心职能为 sequenceId 生成。
 */
public interface IMessageService extends IService<Message> {

    /**
     * 获取下一个序列号（雪花算法）
     */
    Long getNextSequenceId();

    List<Message> getMessages(Long conversationId, int pageNum, int pageSize);

    void markRead(Long userId, Long conversationId);

    /**
     * 代发消息（由 AI 触发工具调用）
     */
    Message sendAgentMessage(Long senderId, Long receiverId, String content, String agentName);
}
