package org.linxin.server.websocket;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 离线消息服务 MySQL 实现
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class MysqlOfflineMessageService implements IOfflineMessageService {

    private final org.linxin.server.module.chat.mapper.MessageMapper messageMapper;

    @Override
    public void saveOfflineMessage(Long userId, Object message) {
        log.debug("Message for user {} handled by ChatService persistence", userId);
    }

    @Override
    public List<Object> fetchAndClearMessages(Long userId) {
        // 在读扩散和 Sequence ID 同步架构下，用户上线后应通过 /chat/sync 接口主动同步离线期间的消息。
        // WebSocket 不再主动推送离线期间的历史消息，以减轻上线瞬间的突发流量压力。
        log.debug("User {} online, waiting for client incremental sync via REST API", userId);
        return Collections.emptyList();
    }
}
