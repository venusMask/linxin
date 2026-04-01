package org.linxin.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.linxin.server.websocket.WebSocketHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RedisMessageSubscriber implements MessageListener, ApplicationContextAware {

    private final ObjectMapper objectMapper;
    private ApplicationContext applicationContext;

    public RedisMessageSubscriber(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            byte[] body = message.getBody();
            Map<String, Object> data = objectMapper.readValue(body, Map.class);
            
            Long userId = Long.valueOf(data.get("userId").toString());
            Object wsMessage = data.get("message");
            
            WebSocketHandler handler = applicationContext.getBean(WebSocketHandler.class);
            handler.pushMessageToLocalUser(userId, wsMessage);
            
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }
}
