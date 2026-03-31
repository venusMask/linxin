package org.linxin.server.auth;

import lombok.Data;

@Data
public class AuthService {

    private JwtService jwtService;

    public String login(String username, String password) {
        return jwtService.generateToken(1L, username);
    }

    public boolean validateToken(String token) {
        try {
            return !jwtService.isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

}
