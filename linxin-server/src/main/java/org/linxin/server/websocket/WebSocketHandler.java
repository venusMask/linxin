package org.linxin.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 消息处理类
 * 
 * 简化架构：由于单机 MySQL 环境，移除了 IMessageBroker 和 IOfflineMessageService 冗余抽象。
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public WebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(userId, session);
            sessionLocks.put(userId, new ReentrantLock());
            log.info("WebSocket connected: user {}", userId);

            // 离线消息拉取由客户端重连后主动调用 /chat/sync 实现，无需后端推送历史消息
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            sessionLocks.remove(userId);
            log.info("WebSocket disconnected: user {}", userId);
        }
    }

    /**
     * 业务调用入口：向指定用户推送消息
     */
    public void sendMessageToUser(Long userId, Object message) {
        log.info("[Trace] Attempting to push message to user {}", userId);
        boolean success = pushMessageToLocalUser(userId, message);
        if (!success) {
            log.info(
                    "[Trace] User {} is offline or session closed, message will be retrieved via /chat/sync on next login",
                    userId);
        } else {
            log.info("[Trace] Message pushed to user {} successfully", userId);
        }
    }

    /**
     * 底层推送实现
     */
    public boolean pushMessageToLocalUser(Long userId, Object message) {
        WebSocketSession session = sessions.get(userId);
        if (session == null) {
            log.info("[Trace] No session found for user {}", userId);
            return false;
        }

        if (!session.isOpen()) {
            log.info("[Trace] Session for user {} exists but is CLOSED", userId);
            return false;
        }

        ReentrantLock lock = sessionLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                return true;
            } else {
                log.info("[Trace] Session for user {} closed just before sending", userId);
            }
        } catch (IOException e) {
            log.error("[Trace] IOException pushing message to user {}: {}", userId, e.getMessage());
        } finally {
            lock.unlock();
        }
        return false;
    }
}
