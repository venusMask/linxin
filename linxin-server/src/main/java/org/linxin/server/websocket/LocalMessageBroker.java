package org.linxin.server.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 本地单机消息经纪人实现（适用于资源有限、单节点部署的情况）
 */
@Slf4j
@Component
@Primary
public class LocalMessageBroker implements IMessageBroker {

    private final WebSocketHandler webSocketHandler;

    public LocalMessageBroker(@Lazy WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void broadcastToUser(Long userId, Object message) {
        // 单机环境下，只需尝试本地推送
        boolean success = webSocketHandler.pushMessageToLocalUser(userId, message);
        if (!success) {
            log.debug("User {} not online locally, message marked as offline (stored in MySQL)", userId);
        }
    }
}
