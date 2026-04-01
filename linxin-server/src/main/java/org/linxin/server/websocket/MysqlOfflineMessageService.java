package org.linxin.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 离线消息服务 MySQL 实现
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class MysqlOfflineMessageService implements IOfflineMessageService {

    private final org.linxin.server.business.mapper.MessageMapper messageMapper;

    @Override
    public void saveOfflineMessage(Long userId, Object message) {
        log.debug("Message for user {} handled by ChatService persistence", userId);
    }

    @Override
    public List<Object> fetchAndClearMessages(Long userId) {
        log.debug("Fetching unread messages for user {} from MySQL", userId);
        List<org.linxin.server.business.vo.MessageVO> unreadMessages = messageMapper.selectUnreadMessages(userId);
        
        if (unreadMessages.isEmpty()) {
            return Collections.emptyList();
        }

        return unreadMessages.stream().map(vo -> {
            String type = vo.getGroupId() != null ? "group_message" : "new_message";
            return new WebSocketMessage(type, vo);
        }).collect(java.util.stream.Collectors.toList());
    }
}
