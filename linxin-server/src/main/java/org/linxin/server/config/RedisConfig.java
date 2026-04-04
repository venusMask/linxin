package org.linxin.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod", "dev"})
public class RedisConfig {

    public static final String MESSAGE_TOPIC = "linxin.websocket.messages";

    // Redis 相关的 Bean 定义被移除，改为在有 Redis 依赖时通过条件配置加载
}
