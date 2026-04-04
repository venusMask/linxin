package org.linxin.server.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.linxin.server.business.entity.AgentToken;
import org.linxin.server.business.mapper.AgentTokenMapper;
import org.linxin.server.business.service.IAgentTokenService;
import org.springframework.stereotype.Service;

@Service
public class AgentTokenServiceImpl extends ServiceImpl<AgentTokenMapper, AgentToken> implements IAgentTokenService {

    @Override
    public AgentToken generateToken(Long userId, String agentName, String scopes, LocalDateTime expireTime) {
        AgentToken agentToken = new AgentToken();
        agentToken.setUserId(userId);
        agentToken.setAgentName(agentName);
        agentToken.setScopes(scopes != null ? scopes : "msg:send");
        agentToken.setToken("lx_at_" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", ""));
        agentToken.setStatus(1);
        agentToken.setExpireTime(expireTime);
        this.save(agentToken);
        return agentToken;
    }

    @Override
    public AgentToken validateToken(String token) {
        LambdaQueryWrapper<AgentToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentToken::getToken, token).eq(AgentToken::getStatus, 1);
        AgentToken agentToken = this.getOne(wrapper);

        if (agentToken != null) {
            // 检查是否过期
            if (agentToken.getExpireTime() != null && agentToken.getExpireTime().isBefore(LocalDateTime.now())) {
                return null;
            }
            // 更新最后使用时间
            agentToken.setLastUsedTime(LocalDateTime.now());
            this.updateById(agentToken);
        }

        return agentToken;
    }

    @Override
    public List<AgentToken> getUserTokens(Long userId) {
        LambdaQueryWrapper<AgentToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentToken::getUserId, userId).orderByDesc(AgentToken::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public void revokeToken(Long userId, Long tokenId) {
        LambdaQueryWrapper<AgentToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentToken::getId, tokenId).eq(AgentToken::getUserId, userId);
        AgentToken token = this.getOne(wrapper);
        if (token != null) {
            token.setStatus(0);
            this.updateById(token);
        }
    }
}
