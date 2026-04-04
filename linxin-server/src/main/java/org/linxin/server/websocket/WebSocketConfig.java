package org.linxin.server.websocket;

import org.linxin.server.auth.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final JwtService jwtService;

    @Value("${websocket.allowed-origins:http://localhost:*}")
    private String allowedOrigins;

    public WebSocketConfig(WebSocketHandler webSocketHandler, JwtService jwtService) {
        this.webSocketHandler = webSocketHandler;
        this.jwtService = jwtService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOrigins == null || allowedOrigins.isBlank()
                ? new String[0]
                : allowedOrigins.trim().split("\\s*,\\s*");
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins(origins)
                .addInterceptors(new WebSocketInterceptor(jwtService));
    }
}
