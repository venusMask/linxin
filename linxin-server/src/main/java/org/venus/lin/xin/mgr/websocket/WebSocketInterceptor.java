package org.venus.lin.xin.mgr.websocket;

import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.venus.lin.xin.mgr.auth.JwtService;

import java.util.Map;

public class WebSocketInterceptor extends HttpSessionHandshakeInterceptor {

    private final JwtService jwtService;

    public WebSocketInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request, 
                                 org.springframework.http.server.ServerHttpResponse response, 
                                 org.springframework.web.socket.WebSocketHandler wsHandler, 
                                 Map<String, Object> attributes) throws Exception {
        // 从请求头获取token
        String token = request.getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                // 验证token并获取用户信息
                Long userId = jwtService.extractUserId(token);
                attributes.put("userId", userId);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
