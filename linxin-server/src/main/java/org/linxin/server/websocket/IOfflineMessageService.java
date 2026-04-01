package org.linxin.server.websocket;

import java.util.List;

/**
 * 离线消息服务接口
 */
public interface IOfflineMessageService {
    /**
     * 存储离线消息
     */
    void saveOfflineMessage(Long userId, Object message);

    /**
     * 获取并删除（或标记已读）用户的离线消息
     */
    List<Object> fetchAndClearMessages(Long userId);
}
