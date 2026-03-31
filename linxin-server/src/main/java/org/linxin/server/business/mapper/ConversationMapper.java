package org.linxin.server.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.linxin.server.business.entity.Conversation;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
