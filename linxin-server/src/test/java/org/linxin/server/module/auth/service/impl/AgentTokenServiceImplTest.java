package org.linxin.server.module.auth.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linxin.server.module.auth.entity.AgentToken;
import org.linxin.server.module.auth.mapper.AgentTokenMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AgentTokenServiceImplTest {

    @Mock
    private AgentTokenMapper agentTokenMapper;

    @InjectMocks
    private AgentTokenServiceImpl agentTokenService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(agentTokenService, "baseMapper", agentTokenMapper);
    }

    private Long userId = 1001L;

    @Test
    public void testGenerateToken() {
        String agentName = "TestAgent";
        String scopes = "msg:send";
        LocalDateTime expire = LocalDateTime.now().plusDays(7);

        AgentToken result = agentTokenService.generateToken(userId, agentName, scopes, expire);

        assertNotNull(result);
        assertTrue(result.getToken().startsWith("lx_at_"));
        assertEquals(userId, result.getUserId());
        verify(agentTokenMapper, times(1)).insert(any(AgentToken.class));
    }

    @Test
    public void testValidateToken_Success() {
        String tokenStr = "lx_at_valid_token";
        AgentToken mockToken = new AgentToken();
        mockToken.setToken(tokenStr);
        mockToken.setUserId(userId);
        mockToken.setStatus(1);
        mockToken.setExpireTime(LocalDateTime.now().plusDays(1));

        // 使用 lenient() 避免 MyBatis-Plus 内部多参数调用导致的严格匹配失败
        lenient().when(agentTokenMapper.selectOne(any(), anyBoolean())).thenReturn(mockToken);
        lenient().when(agentTokenMapper.selectOne(any())).thenReturn(mockToken);

        AgentToken result = agentTokenService.validateToken(tokenStr);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    public void testValidateToken_Expired() {
        String tokenStr = "lx_at_expired_token";
        AgentToken mockToken = new AgentToken();
        mockToken.setToken(tokenStr);
        mockToken.setStatus(1);
        mockToken.setExpireTime(LocalDateTime.now().minusDays(1));

        lenient().when(agentTokenMapper.selectOne(any(), anyBoolean())).thenReturn(mockToken);

        AgentToken result = agentTokenService.validateToken(tokenStr);

        assertNull(result);
    }

    @Test
    public void testValidateToken_NotFound() {
        lenient().when(agentTokenMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        AgentToken result = agentTokenService.validateToken("invalid");
        assertNull(result);
    }

    @Test
    public void testRevokeToken() {
        Long tokenId = 500L;
        AgentToken mockToken = new AgentToken();
        mockToken.setId(tokenId);
        mockToken.setUserId(userId);
        mockToken.setStatus(1);

        lenient().when(agentTokenMapper.selectOne(any(), anyBoolean())).thenReturn(mockToken);

        agentTokenService.revokeToken(userId, tokenId);

        ArgumentCaptor<AgentToken> captor = ArgumentCaptor.forClass(AgentToken.class);
        verify(agentTokenMapper).updateById((AgentToken) captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }
}
