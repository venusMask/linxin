package org.linxin.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<TextMessage>> pendingMessages = new ConcurrentHashMap<>();
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
            pendingMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
            
            sendPendingMessages(userId, session);
            
            log.info("WebSocket connection established for user: {}", userId);
        }
    }
    
    private void sendPendingMessages(Long userId, WebSocketSession session) {
        ConcurrentLinkedQueue<TextMessage> queue = pendingMessages.get(userId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        
        ReentrantLock lock = getSessionLock(userId);
        lock.lock();
        try {
            TextMessage message;
            while ((message = queue.poll()) != null) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                        log.debug("Sent pending message to user: {}", userId);
                    } catch (IOException e) {
                        log.error("Error sending pending message to user {}", userId, e);
                        queue.offer(message);
                        break;
                    }
                } else {
                    queue.offer(message);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received message: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            sessionLocks.remove(userId);
            log.info("WebSocket connection closed for user: {}", userId);
        }
    }

    private ReentrantLock getSessionLock(Long userId) {
        return sessionLocks.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    public void sendMessageToUser(Long userId, Object message) {
        ConcurrentLinkedQueue<TextMessage> messageQueue = pendingMessages.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        WebSocketSession session = sessions.get(userId);

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            ReentrantLock lock = getSessionLock(userId);
            lock.lock();
            try {
                if (session != null && session.isOpen()) {
                    session.sendMessage(textMessage);
                    log.debug("Sent message to user: {}", userId);
                } else {
                    messageQueue.offer(textMessage);
                    log.warn("Session not found or closed, message queued for user: {}", userId);
                }
            } finally {
                lock.unlock();
            }
        } catch (IOException e) {
            log.error("Error sending message to user {}", userId, e);
        }
    }

    public void broadcastMessage(Object message) {
        for (Long userId : sessions.keySet()) {
            sendMessageToUser(userId, message);
        }
    }

    public int getOnlineUserCount() {
        return sessions.size();
    }
}
