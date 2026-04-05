package org.linxin.server.module.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import org.linxin.server.module.auth.entity.AgentToken;

public interface IAgentTokenService extends IService<AgentToken> {
    AgentToken generateToken(Long userId, String agentName, String scopes, java.time.LocalDateTime expireTime);
    AgentToken validateToken(String token);
    List<AgentToken> getUserTokens(Long userId);
    void revokeToken(Long userId, Long tokenId);
}
