package org.linxin.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final IMessageBroker messageBroker;
    private final IOfflineMessageService offlineMessageService;

    public WebSocketHandler(ObjectMapper objectMapper, IMessageBroker messageBroker,
            IOfflineMessageService offlineMessageService) {
        this.objectMapper = objectMapper;
        this.messageBroker = messageBroker;
        this.offlineMessageService = offlineMessageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.put(userId, session);
            sessionLocks.put(userId, new ReentrantLock());

            log.info("WebSocket connected: user {}", userId);

            // 离线消息拉取逻辑抽象
            List<Object> pendingMessages = offlineMessageService.fetchAndClearMessages(userId);
            for (Object msg : pendingMessages) {
                pushMessageToLocalUser(userId, msg);
            }
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
     * 业务调用入口
     */
    public void sendMessageToUser(Long userId, Object message) {
        // 委托给经纪人处理跨机/本地分发
        messageBroker.broadcastToUser(userId, message);
    }

    /**
     * 底层推送实现
     */
    public boolean pushMessageToLocalUser(Long userId, Object message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            ReentrantLock lock = sessionLocks.computeIfAbsent(userId, k -> new ReentrantLock());
            lock.lock();
            try {
                if (session.isOpen()) {
                    String json = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                    return true;
                }
            } catch (IOException e) {
                log.error("Failed to push message to user {}", userId, e);
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
