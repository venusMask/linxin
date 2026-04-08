package org.linxin.server.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.linxin.server.module.chat.entity.Message;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    @Select("SELECT MAX(seq) FROM (" +
            "  SELECT MAX(sequence_id) AS seq FROM messages WHERE sender_id = #{userId} " +
            "  UNION ALL " +
            "  SELECT MAX(sequence_id) AS seq FROM messages WHERE receiver_id = #{userId} " +
            "  UNION ALL " +
            "  SELECT MAX(m.sequence_id) AS seq FROM messages m " +
            "  INNER JOIN group_members gm ON m.group_id = gm.group_id " +
            "  WHERE gm.user_id = #{userId} AND gm.deleted = 0" +
            ") t")
    Long selectMaxSequenceId(@Param("userId") Long userId);
}
