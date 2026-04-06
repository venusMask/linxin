package org.linxin.server.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.linxin.server.module.chat.entity.Message;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    @Select("SELECT MAX(sequence_id) FROM messages WHERE sender_id = #{userId} OR receiver_id = #{userId}")
    Long selectMaxSequenceId(@Param("userId") Long userId);
}
