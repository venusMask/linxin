package org.linxin.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisMessageSubscriber {

    private final ObjectMapper objectMapper;

    public RedisMessageSubscriber(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Redis 相关的方法被移除，改为在有 Redis 依赖时通过条件配置加载
}
