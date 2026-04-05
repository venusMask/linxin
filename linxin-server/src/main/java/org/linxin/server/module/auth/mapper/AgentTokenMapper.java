package org.linxin.server.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.linxin.server.module.auth.entity.AgentToken;

@Mapper
public interface AgentTokenMapper extends BaseMapper<AgentToken> {
}
