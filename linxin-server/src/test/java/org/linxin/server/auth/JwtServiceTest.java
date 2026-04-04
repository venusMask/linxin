package org.linxin.server.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private AuthConfig authConfig;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    public void setup() {
        authConfig = new AuthConfig();
        authConfig.setSecret(secret);
        authConfig.setExpire(1440);
        jwtService = new JwtService(authConfig);
    }

    @Test
    public void testGenerateAndExtract() {
        Long userId = 123L;
        String username = "testuser";

        String token = jwtService.generateToken(userId, username);
        assertNotNull(token);

        String extractedUsername = jwtService.extractUsername(token);
        Long extractedUserId = jwtService.extractUserId(token);

        assertEquals(username, extractedUsername);
        assertEquals(userId, extractedUserId);
    }

    @Test
    public void testValidateToken() {
        Long userId = 123L;
        String username = "testuser";
        String token = jwtService.generateToken(userId, username);

        assertTrue(jwtService.validateToken(token, username));
        assertFalse(jwtService.validateToken(token, "otheruser"));
    }
}
