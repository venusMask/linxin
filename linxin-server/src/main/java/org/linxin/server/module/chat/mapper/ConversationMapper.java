package org.linxin.server.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.linxin.server.module.chat.entity.Conversation;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
