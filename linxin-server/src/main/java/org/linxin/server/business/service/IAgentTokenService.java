package org.linxin.server.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.linxin.server.business.entity.AgentToken;

import java.util.List;

public interface IAgentTokenService extends IService<AgentToken> {
    AgentToken generateToken(Long userId, String agentName, String scopes, java.time.LocalDateTime expireTime);
    AgentToken validateToken(String token);
    List<AgentToken> getUserTokens(Long userId);
    void revokeToken(Long userId, Long tokenId);
}
